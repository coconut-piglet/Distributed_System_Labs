/*
 * FILE: rdt_receiver.cc
 * DESCRIPTION: Reliable data transfer receiver.
 * NOTE: This implementation assumes there is no packet loss, corruption, or 
 *       reordering.  You will need to enhance it to deal with all these 
 *       situations.  In this implementation, the packet format is laid out as 
 *       the following:
 *       
 *       |<-  2 byte  ->|<-   2 byte   ->|<-    1 byte    ->|<-            the rest           ->|
 *       |<- checksum ->|<- pkt number ->|<- payload size ->|<-            payload            ->|
 *
 *       The first byte of each packet indicates the size of the payload
 *       (excluding this single-byte header)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <algorithm>
#include <deque>

#include "rdt_struct.h"
#include "rdt_receiver.h"

/* predefined variables */
#define BUFFERSIZE 8
#define DEBUG 1

using namespace std;

/* receiver pkt sequence */
unsigned short next_num;

/* pkt buffer */
deque<struct packet> received_pkt_buffer;
deque<unsigned short> received_num_buffer;

/* receiver initialization, called once at the very beginning */
void Receiver_Init()
{
    if (DEBUG)
        fprintf(stdout, "At %.2fs: receiver initializing ...\n", GetSimulationTime());
    next_num = 0;
}

/* receiver finalization, called once at the very end.
   you may find that you don't need it, in which case you can leave it blank.
   in certain cases, you might want to use this opportunity to release some 
   memory you allocated in Receiver_init(). */
void Receiver_Final()
{
    if (DEBUG)
        fprintf(stdout, "At %.2fs: receiver finalizing ...\n", GetSimulationTime());
}

/* receiver checksum */
unsigned short Receiver_Checksum(struct packet *pkt)
{
    unsigned short checksum = 0;
    unsigned int tmp = 0;
    /* since the RDT_PKTSIZE is a even number, this will cover the whole pkt */
    for (int i = 2; i < RDT_PKTSIZE; i += 2)
    {
        tmp += *(unsigned short *)(&(pkt->data[i]));
    }
    tmp = (tmp >> 16) + (tmp & 0xffff);
    tmp += (tmp >> 16);
    checksum = ~tmp;
    return checksum;
}

/* receiver send ack to sender 
   |<-  2 byte  ->|<-   2 byte  - >|<-             the rest            ->| 
   |<- checksum ->|<- pkt number ->|<-               none              ->| */
void Receiver_SendACK(unsigned short ack_num)
{
    packet ackpkt;
    memcpy(&ackpkt.data[2], &ack_num, sizeof(unsigned short));
    unsigned short checksum = Receiver_Checksum(&ackpkt);
    memcpy(ackpkt.data, &checksum, sizeof(unsigned short));
    Receiver_ToLowerLayer(&ackpkt);
}

void Receiver_DisplayBufferStatus()
{
    fprintf(stdout, "At %.2fs: receiver buffers size: %lu\n", GetSimulationTime(), received_num_buffer.size());
    fprintf(stdout, "At %.2fs: receiver buffers head: %u\n", GetSimulationTime(), next_num);
    fprintf(stdout, "At %.2fs: receiver buffers content: |", GetSimulationTime());
    for (unsigned int i = 0; i < received_num_buffer.size(); i++)
    {
        fprintf(stdout, "%u|", received_num_buffer[i]);
    }
    fprintf(stdout, "\n");
}

/* event handler, called when a packet is passed from the lower layer at the 
   receiver */
void Receiver_FromLowerLayer(struct packet *pkt)
{
    /* perform checksum before further operation */
    unsigned int checksum;
    checksum = Receiver_Checksum(pkt);
    if (memcmp(&checksum, pkt, sizeof(unsigned short)) != 0)
    {
        if (DEBUG)
            fprintf(stdout, "At %.2fs: receiver receives a corrupted packet\n", GetSimulationTime());
        return;
    }

    /* extract msg number and pkt number */
    unsigned short pkt_num;
    memcpy(&pkt_num, &pkt->data[2], sizeof(unsigned short));
    if (DEBUG)
        fprintf(stdout, "At %.2fs: receiver receives packet %u\n", GetSimulationTime(), pkt_num);

    /* perform pkt_num check */
    if (pkt_num > next_num + BUFFERSIZE - 1)
    {
        /* check whether this packet arrives too soon, if so, drop it without sending ACK */
        if (DEBUG)
            fprintf(stdout, "At %.2fs: packet %u reaches too early, can not store packet later than %u\n", GetSimulationTime(), pkt_num, next_num + BUFFERSIZE - 1);
        return;
    }
    else if (pkt_num < next_num)
    {
        /* check whether this packet is a duplicate of certain previous packet */
        if (DEBUG)
            fprintf(stdout, "At %.2fs: got a duplicate of already received packet %u, drop it\n", GetSimulationTime(), pkt_num);
        /* send ACK in case the previous ACK is corrupted */
        Receiver_SendACK(pkt_num);
        return;
    }
    else if (find(received_num_buffer.begin(), received_num_buffer.end(), pkt_num) != received_num_buffer.end())
    {
        /* check whether this packet is a duplicate of certain buffered packet */
        if (DEBUG)
            fprintf(stdout, "At %.2fs: got a duplicate of already buffered packet %u, drop it\n", GetSimulationTime(), pkt_num);
        /* send ACK in case the previous ACK is corrupted */
        Receiver_SendACK(pkt_num);
        return;
    }

    /* send ACK to sender when a complete new packet arrives */
    Receiver_SendACK(pkt_num);

    /* the capacity left is checked by the caller, so it's safe to add it to buffer here */
    if (DEBUG)
        fprintf(stdout, "At %.2fs: receiver buffers packet %u\n", GetSimulationTime(), pkt_num);
    received_num_buffer.push_front(pkt_num);
    received_pkt_buffer.push_front(*pkt);

    /* but the order needs to be rearranged */
    for (unsigned int i = 1; i < received_num_buffer.size(); i++)
    {
        if (received_num_buffer[i] < received_num_buffer[i - 1])
        {
            swap(received_num_buffer[i], received_num_buffer[i - 1]);
            swap(received_pkt_buffer[i], received_pkt_buffer[i - 1]);
        }
        else
        {
            break;
        }
    }

    /* display buffer status */
    if (DEBUG)
        Receiver_DisplayBufferStatus();

    /* 5-byte header indicating the checksum and sequence of the packet and the size of the payload */
    int header_size = 5;

    /* then check whether there exists continuous packets start from next_num */
    while (received_num_buffer.front() == next_num)
    {
        /* construct a message and deliver to the upper layer */
        struct message *msg = (struct message *)malloc(sizeof(struct message));
        ASSERT(msg != NULL);

        msg->size = received_pkt_buffer.front().data[4];

        /* sanity check in case the packet is corrupted */
        if (msg->size < 0)
            msg->size = 0;
        if (msg->size > RDT_PKTSIZE - header_size)
            msg->size = RDT_PKTSIZE - header_size;

        msg->data = (char *)malloc(msg->size);
        ASSERT(msg->data != NULL);
        memcpy(msg->data, received_pkt_buffer.front().data + header_size, msg->size);
        Receiver_ToUpperLayer(msg);

        /* remove the packet from buffer and increase the next_num */
        if (DEBUG)
            fprintf(stdout, "At %.2fs: buffer head arrives, shift buffer\n", GetSimulationTime());
        received_num_buffer.pop_front();
        received_pkt_buffer.pop_front();
        next_num++;

        /* don't forget to free the space */
        if (msg->data != NULL)
            free(msg->data);
        if (msg != NULL)
            free(msg);
    }
}

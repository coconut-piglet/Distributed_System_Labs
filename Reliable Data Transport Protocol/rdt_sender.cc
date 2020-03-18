/*
 * FILE: rdt_sender.cc
 * DESCRIPTION: Reliable data transfer sender.
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
#include <deque>

#include "rdt_struct.h"
#include "rdt_sender.h"

/* predefined variables */
#define TIMEOUT 0.3
#define WINDOWSIZE 8
#define DEBUG 1

using namespace std;

/* sender ack flag */
bool sender_ack;

/* sender pkt sequence */
unsigned short pkt_num;

/* pkt buffer */
deque<struct packet> pkt_buffer;
deque<unsigned short> num_buffer;
deque<bool> ack_buffer;

/* sender initialization, called once at the very beginning */
void Sender_Init()
{
    if (DEBUG)
        fprintf(stdout, "At %.2fs: sender initializing ...\n", GetSimulationTime());
    sender_ack = false;

    /* packet number starts from zero */
    pkt_num = 0;
}

/* sender finalization, called once at the very end.
   you may find that you don't need it, in which case you can leave it blank.
   in certain cases, you might want to take this opportunity to release some 
   memory you allocated in Sender_init(). */
void Sender_Final()
{
    if (DEBUG)
        fprintf(stdout, "At %.2fs: sender finalizing ...\n", GetSimulationTime());

    /* clean all buffered data */
    pkt_buffer.clear();
    num_buffer.clear();
    ack_buffer.clear();
}

/* sender checksum */
unsigned short Sender_Checksum(struct packet *pkt)
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

/* display current buffer status*/
void Sender_DisplayBufferStatus()
{
    fprintf(stdout, "At %.2fs: sender pkt buffers size: %lu\n", GetSimulationTime(), pkt_buffer.size());
    fprintf(stdout, "At %.2fs: sender num buffers size: %lu\n", GetSimulationTime(), num_buffer.size());
    fprintf(stdout, "At %.2fs: sender ack buffers size: %lu\n", GetSimulationTime(), ack_buffer.size());
    int buffered_pkt_num = WINDOWSIZE > num_buffer.size() ? num_buffer.size() : WINDOWSIZE;
    fprintf(stdout, "At %.2fs: sender buffers content: |", GetSimulationTime());
    for (int i = 0; i < buffered_pkt_num; i++)
    {
        fprintf(stdout, "%u|", num_buffer[i]);
    }
    fprintf(stdout, "\n");
    fprintf(stdout, "At %.2fs: sender buffers ack status: |", GetSimulationTime());
    for (int i = 0; i < buffered_pkt_num; i++)
    {
        if (ack_buffer[i])
        {
            fprintf(stdout, "true|");
        }
        else
        {
            fprintf(stdout, "false|");
        }
    }
    fprintf(stdout, "\n");
}

/* sender handle new buffer */
void Sender_HandleBufferChange(struct packet *pkt, unsigned int pkt_num)
{
    /* add new packet and its number to their buffer */
    pkt_buffer.push_back(*pkt);
    num_buffer.push_back(pkt_num);
    ack_buffer.push_back(false);
    if (DEBUG)
        Sender_DisplayBufferStatus();

    /* if buffer is not filled before insertion, send the newly added packet */
    if (num_buffer.size() <= WINDOWSIZE)
    {
        Sender_StartTimer(TIMEOUT);
        if (DEBUG)
            fprintf(stdout, "At %.2fs: sender send packet %u\n", GetSimulationTime(), num_buffer.back());
        Sender_ToLowerLayer(&pkt_buffer.back());
    }
}

/* event handler, called when a message is passed from the upper layer at the 
   sender */
void Sender_FromUpperLayer(struct message *msg)
{
    /* 5-byte header indicating the checksum and sequence of the packet and the size of the payload */
    int header_size = 5;

    /* maximum payload size */
    int maxpayload_size = RDT_PKTSIZE - header_size;

    /* split the message if it is too big */

    /* reuse the same packet data structure */
    packet pkt;

    /* initiate checksum */
    unsigned short checksum;

    /* the cursor always points to the first unsent byte in the message */
    int cursor = 0;

    while (msg->size - cursor > maxpayload_size)
    {
        /* fill in the packet */
        pkt.data[4] = maxpayload_size;
        memcpy(pkt.data + header_size, msg->data + cursor, maxpayload_size);
        memcpy(&pkt.data[2], &pkt_num, sizeof(unsigned short));

        /* add checksum to the packet */
        checksum = Sender_Checksum(&pkt);
        memcpy(pkt.data, &checksum, sizeof(unsigned short));

        /* add it to buffer */
        Sender_HandleBufferChange(&pkt, pkt_num);

        /* move the cursor */
        cursor += maxpayload_size;

        /* increase the pkt number */
        pkt_num++;
    }

    /* send out the last packet */
    if (msg->size > cursor)
    {
        /* fill in the packet */
        pkt.data[4] = msg->size - cursor;
        memcpy(pkt.data + header_size, msg->data + cursor, pkt.data[4]);
        memcpy(&pkt.data[2], &pkt_num, sizeof(unsigned short));

        /* add checksum to the packet */
        checksum = Sender_Checksum(&pkt);
        memcpy(pkt.data, &checksum, sizeof(unsigned short));

        /* add it to buffer */
        Sender_HandleBufferChange(&pkt, pkt_num);

        /* increase the pkt number */
        pkt_num++;
    }
}

/* event handler, called when a packet is passed from the lower layer at the 
   sender */
void Sender_FromLowerLayer(struct packet *pkt)
{
    /* perform checksum before further operation */
    unsigned short checksum;
    checksum = Sender_Checksum(pkt);
    if (memcmp(&checksum, pkt, sizeof(unsigned short)) != 0)
    {
        if (DEBUG)
            fprintf(stdout, "At %.2fs: sender receives a corrupted ACK\n", GetSimulationTime());
        return;
    }

    /* extract ack_num from ACK packet */
    unsigned short ack_num;
    memcpy(&ack_num, &pkt->data[2], sizeof(unsigned short));
    if (DEBUG)
        fprintf(stdout, "At %.2fs: sender receives ACK %u\n", GetSimulationTime(), ack_num);

    /* if the first packet in the buffer got its ACK */
    if (ack_num == num_buffer.front())
    {
        /* remove continuous acknowledged packet from the buffer and shift the window */
        ack_buffer.front() = true;
        int windows_shift_times = 0;
        while (ack_buffer.front())
        {
            num_buffer.pop_front();
            pkt_buffer.pop_front();
            ack_buffer.pop_front();
            windows_shift_times++;
            if (ack_buffer.empty())
                break;
        }

        if (DEBUG)
            Sender_DisplayBufferStatus();

        /* send newly entered packets, up to windows_shift_times */
        int buffered_pkt_num = WINDOWSIZE > num_buffer.size() ? num_buffer.size() : WINDOWSIZE;
        for (int i = WINDOWSIZE - windows_shift_times; i < buffered_pkt_num; i++)
        {
            Sender_StartTimer(TIMEOUT);
            Sender_ToLowerLayer(&pkt_buffer[i]);
        }
    }
    else
    {
        /* if other packet in the buffer got its ACK before the first one, mark it so that it won't be send twice */
        int buffered_pkt_num = WINDOWSIZE > num_buffer.size() ? num_buffer.size() : WINDOWSIZE;
        for (int i = 0; i < buffered_pkt_num; i++)
        {
            if (num_buffer[i] == ack_num)
            {
                ack_buffer[i] = true;
                break;
            }
        }
    }
}

/* event handler, called when the timer expires */
void Sender_Timeout()
{
    if (DEBUG)
        fprintf(stdout, "At %.2fs: sender times out\n", GetSimulationTime());
    if (num_buffer.size() == 0)
    {
        if (DEBUG)
            fprintf(stdout, "At %.2fs: no packet in buffer, timer stops\n", GetSimulationTime());
        return;
    }

    /* restart the timer */
    Sender_StartTimer(TIMEOUT);

    /* resend selective packet in buffered area */
    int buffered_pkt_num = WINDOWSIZE > num_buffer.size() ? num_buffer.size() : WINDOWSIZE;
    for (int i = 0; i < buffered_pkt_num; i++)
    {
        /* if packet has not received its ACK, resend it, otherwise do nothing */
        if (!ack_buffer[i])
        {
            if (DEBUG)
                fprintf(stdout, "At %.2fs: sender resend packet %u\n", GetSimulationTime(), num_buffer[i]);
            Sender_ToLowerLayer(&pkt_buffer[i]);
        }
    }
}

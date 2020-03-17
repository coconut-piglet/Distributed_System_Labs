/*
 * FILE: rdt_sender.cc
 * DESCRIPTION: Reliable data transfer sender.
 * NOTE: This implementation assumes there is no packet loss, corruption, or 
 *       reordering.  You will need to enhance it to deal with all these 
 *       situations.  In this implementation, the packet format is laid out as 
 *       the following:
 *       
 *       |<-  2 byte  ->|<-  1 byte  ->|<-             the rest            ->|
 *       |<- checksum ->| payload size |<-             payload             ->|
 *
 *       The first byte of each packet indicates the size of the payload
 *       (excluding this single-byte header)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "rdt_struct.h"
#include "rdt_sender.h"

/* predefined variables */
#define TIMEOUT 0.3

/* sender ack flag */
bool sender_ack;

/* sender initialization, called once at the very beginning */
void Sender_Init()
{
    fprintf(stdout, "At %.2fs: sender initializing ...\n", GetSimulationTime());
    sender_ack = false;
}

/* sender finalization, called once at the very end.
   you may find that you don't need it, in which case you can leave it blank.
   in certain cases, you might want to take this opportunity to release some 
   memory you allocated in Sender_init(). */
void Sender_Final()
{
    fprintf(stdout, "At %.2fs: sender finalizing ...\n", GetSimulationTime());
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

/* event handler, called when a message is passed from the upper layer at the 
   sender */
void Sender_FromUpperLayer(struct message *msg)
{
    /* 3-byte header indicating the checksum of the packet and the size of the payload */
    int header_size = 3;

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
        pkt.data[2] = maxpayload_size;
        memcpy(pkt.data + header_size, msg->data + cursor, maxpayload_size);

        /* add checksum to the packet */
        checksum = Sender_Checksum(&pkt);
        memcpy(pkt.data, &checksum, sizeof(unsigned short));

        /* send it out through the lower layer */
        Sender_ToLowerLayer(&pkt);

        /* move the cursor */
        cursor += maxpayload_size;
    }

    /* send out the last packet */
    if (msg->size > cursor)
    {
        /* fill in the packet */
        pkt.data[2] = msg->size - cursor;
        memcpy(pkt.data + header_size, msg->data + cursor, pkt.data[2]);

        /* add checksum to the packet */
        checksum = Sender_Checksum(&pkt);
        memcpy(pkt.data, &checksum, sizeof(unsigned short));

        /* send it out through the lower layer */
        Sender_ToLowerLayer(&pkt);
    }
}

/* event handler, called when a packet is passed from the lower layer at the 
   sender */
void Sender_FromLowerLayer(struct packet *pkt)
{
    unsigned short checksum;
    checksum = Sender_Checksum(pkt);
    if (memcmp(&checksum, pkt, sizeof(unsigned short)) != 0)
    {
        fprintf(stdout, "At %.2fs: sender receives a corrupted ACK\n", GetSimulationTime());
        return;
    }
    //fprintf(stdout, "At %.2fs: sender receives a complete ACK\n", GetSimulationTime());

    int ack_num;
    memcpy(&ack_num, &pkt->data[2], sizeof(int));
    /* TODO: inform the sender */
}

/* event handler, called when the timer expires */
void Sender_Timeout()
{
}

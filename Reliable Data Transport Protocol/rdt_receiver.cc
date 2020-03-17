/*
 * FILE: rdt_receiver.cc
 * DESCRIPTION: Reliable data transfer receiver.
 * NOTE: This implementation assumes there is no packet loss, corruption, or 
 *       reordering.  You will need to enhance it to deal with all these 
 *       situations.  In this implementation, the packet format is laid out as 
 *       the following:
 *       
 *       |<-  2 byte  ->|<-  2 byte  ->|<-  2 byte  ->|<-  1 byte  ->|<-             the rest            ->|
 *       |<- checksum ->|  msg number  |  pkt number  | payload size |<-             payload             ->|
 *
 *       The first byte of each packet indicates the size of the payload
 *       (excluding this single-byte header)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "rdt_struct.h"
#include "rdt_receiver.h"

/* receiver initialization, called once at the very beginning */
void Receiver_Init()
{
    fprintf(stdout, "At %.2fs: receiver initializing ...\n", GetSimulationTime());
}

/* receiver finalization, called once at the very end.
   you may find that you don't need it, in which case you can leave it blank.
   in certain cases, you might want to use this opportunity to release some 
   memory you allocated in Receiver_init(). */
void Receiver_Final()
{
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
   |<-  2 byte  ->|<-  4 byte  ->|<-             the rest            ->| 
   |<- checksum ->|<- reserved ->|<-               none              ->| */
void Receiver_SendACK(unsigned int ack_num)
{
    packet ackpkt;
    memcpy(&ackpkt.data[2], &ack_num, sizeof(unsigned int));
    unsigned short checksum = Receiver_Checksum(&ackpkt);
    memcpy(ackpkt.data, &checksum, sizeof(unsigned short));
    Receiver_ToLowerLayer(&ackpkt);
}

/* event handler, called when a packet is passed from the lower layer at the 
   receiver */
void Receiver_FromLowerLayer(struct packet *pkt)
{
    /* 3-byte header indicating the checksum of the packet and the size of the payload */
    int header_size = 7;

    /* TODO: perform checksum before further operation */
    unsigned int checksum;
    checksum = Receiver_Checksum(pkt);
    if (memcmp(&checksum, pkt, sizeof(unsigned short)) != 0)
    {
        fprintf(stdout, "At %.2fs: receiver receives a corrupted packet\n", GetSimulationTime());
        return;
    }

    /* extract msg number and pkt number */
    unsigned short msg_num, pkt_num;
    memcpy(&msg_num, &pkt->data[2], sizeof(unsigned short));
    memcpy(&pkt_num, &pkt->data[4], sizeof(unsigned short));
    fprintf(stdout, "At %.2fs: receiver receives packet %u of message %u\n", GetSimulationTime(), pkt_num, msg_num);

    /* send ACK to sender when a complete packet arrives */
    Receiver_SendACK(0);

    /* construct a message and deliver to the upper layer */
    struct message *msg = (struct message *)malloc(sizeof(struct message));
    ASSERT(msg != NULL);

    msg->size = pkt->data[6];

    /* sanity check in case the packet is corrupted */
    if (msg->size < 0)
        msg->size = 0;
    if (msg->size > RDT_PKTSIZE - header_size)
        msg->size = RDT_PKTSIZE - header_size;

    msg->data = (char *)malloc(msg->size);
    ASSERT(msg->data != NULL);
    memcpy(msg->data, pkt->data + header_size, msg->size);
    Receiver_ToUpperLayer(msg);

    /* don't forget to free the space */
    if (msg->data != NULL)
        free(msg->data);
    if (msg != NULL)
        free(msg);
}

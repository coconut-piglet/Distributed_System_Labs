/* 
 * FILE: rdt_receiver.cc
 * DESCRIPTION: UDP Packet Sender
 */

#include <stdint.h>
#include <inttypes.h>
#include <rte_eal.h>
#include <rte_ethdev.h>
#include <rte_cycles.h>
#include <rte_lcore.h>
#include <rte_mbuf.h>
#include <rte_udp.h>
#include <rte_ether.h>
#include <rte_ip.h>

#define RX_RING_SIZE 1024
#define TX_RING_SIZE 1024

#define NUM_MBUFS 8191
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32

struct rte_data
{
	char content[16];
};

struct Packet
{
	struct rte_ether_hdr ether_hdr;
	struct rte_ipv4_hdr ipv4_hdr;
	struct rte_udp_hdr udp_hdr;
	struct rte_data data;
};

static const struct rte_eth_conf port_conf_default = {
	.rxmode = {
		.max_rx_pkt_len = RTE_ETHER_MAX_LEN,
	},
};

static inline int
port_init(uint16_t port, struct rte_mempool *mbuf_pool)
{
	struct rte_eth_conf port_conf = port_conf_default;
	const uint16_t rx_rings = 1, tx_rings = 1;
	uint16_t nb_rxd = RX_RING_SIZE;
	uint16_t nb_txd = TX_RING_SIZE;
	int retval;
	uint16_t q;

	/* Configure the Ethernet device. */
	retval = rte_eth_dev_configure(0, rx_rings, tx_rings, &port_conf);
	if (retval != 0)
		return retval;

	/* Allocate and set up 1 RX queue per Ethernet port. */
	for (q = 0; q < rx_rings; q++)
	{
		retval = rte_eth_rx_queue_setup(port, q, nb_rxd,
										rte_eth_dev_socket_id(port), NULL, mbuf_pool);
		if (retval < 0)
			return retval;
	}

	/* Allocate and set up 1 TX queue per Ethernet port. */
	for (q = 0; q < tx_rings; q++)
	{
		retval = rte_eth_tx_queue_setup(port, q, nb_txd,
										rte_eth_dev_socket_id(port), NULL);
		if (retval < 0)
			return retval;
	}

	/* Start the Ethernet port. */
	retval = rte_eth_dev_start(port);
	if (retval < 0)
		return retval;

	return 0;
}

static inline void
makePacket(struct Packet *pkt)
{
	pkt->data.content[0] = 'H';
	pkt->data.content[1] = 'e';
	pkt->data.content[2] = 'l';
	pkt->data.content[3] = 'l';
	pkt->data.content[4] = 'o';
	pkt->data.content[5] = ',';
	pkt->data.content[6] = 'W';
	pkt->data.content[7] = 'i';
	pkt->data.content[8] = 'r';
	pkt->data.content[9] = 'e';
	pkt->data.content[10] = 's';
	pkt->data.content[11] = 'h';
	pkt->data.content[12] = 'a';
	pkt->data.content[13] = 'r';
	pkt->data.content[14] = 'k';
	pkt->data.content[15] = '!';

	pkt->udp_hdr.src_port = htons(1080);
	pkt->udp_hdr.dst_port = htons(1080);
	pkt->udp_hdr.dgram_len = htons(sizeof(struct rte_data));
	pkt->udp_hdr.dgram_cksum = 0;

	pkt->ipv4_hdr.version_ihl = 0x45;
	pkt->ipv4_hdr.type_of_service = 0x00;
	pkt->ipv4_hdr.total_length = htons(20 + (rte_be16_t)(sizeof(struct rte_udp_hdr)) + (rte_be16_t)(sizeof(struct rte_data)));
	pkt->ipv4_hdr.packet_id = 1;
	pkt->ipv4_hdr.fragment_offset = 0;
	pkt->ipv4_hdr.time_to_live = 64;
	pkt->ipv4_hdr.next_proto_id = 17;
	inet_pton(AF_INET, "192.168.236.1", &(pkt->ipv4_hdr.src_addr));
	inet_pton(AF_INET, "192.168.50.76", &(pkt->ipv4_hdr.dst_addr));
	pkt->ipv4_hdr.hdr_checksum = rte_ipv4_cksum(&(pkt->ipv4_hdr));

	rte_eth_random_addr(pkt->ether_hdr.s_addr.addr_bytes);
	rte_eth_random_addr(pkt->ether_hdr.d_addr.addr_bytes);
	pkt->ether_hdr.ether_type = htons(RTE_ETHER_TYPE_IPV4);
}

int main(int argc, char *argv[])
{
	struct rte_mempool *mbuf_pool;

	/* Initialize the Environment Abstraction Layer (EAL). */
	int ret = rte_eal_init(argc, argv);
	if (ret < 0)
		rte_exit(EXIT_FAILURE, "Error with EAL initialization\n");

	argc -= ret;
	argv += ret;

	/* Creates a new mempool in memory to hold the mbufs. */
	mbuf_pool = rte_pktmbuf_pool_create("MBUF_POOL", NUM_MBUFS,
										MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());

	if (mbuf_pool == NULL)
		rte_exit(EXIT_FAILURE, "Cannot create mbuf pool\n");

	/* Initialize port 0 */
	if (port_init(0, mbuf_pool) != 0)
		rte_exit(EXIT_FAILURE, "Cannot init port %" PRIu16 "\n", 0);

	struct Packet *pkt;
	struct rte_mbuf *tx_pkts[BURST_SIZE];

	for (int i = 0; i < BURST_SIZE; i++)
	{
		tx_pkts[i] = rte_pktmbuf_alloc(mbuf_pool);
		pkt = rte_pktmbuf_mtod(tx_pkts[i], struct Packet *);
		makePacket(pkt);
		tx_pkts[i]->pkt_len = sizeof(struct Packet);
		tx_pkts[i]->data_len = sizeof(struct Packet);
	}

	uint16_t nb_tx = rte_eth_tx_burst(0, 0, tx_pkts, BURST_SIZE);
	printf("%d packets sent\n", nb_tx);

	for (int i = 0; i < BURST_SIZE; i++)
	{
		rte_pktmbuf_free(tx_pkts[i]);
	}

	return 0;
}

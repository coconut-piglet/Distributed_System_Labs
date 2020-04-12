#include <stdint.h>
#include "qos.h"

/* copy headers from qos_meter/main.c */
#include <rte_common.h>
#include <rte_eal.h>
#include <rte_malloc.h>
#include <rte_mempool.h>
#include <rte_ethdev.h>
#include <rte_cycles.h>
#include <rte_mbuf.h>
#include <rte_meter.h>

/* add header for WRED support */
#include <rte_red.h>

/* define PARAMS, use default value from qos_meter for now */
struct rte_meter_srtcm_params app_srtcm_params[APP_FLOWS_MAX] = {
    {.cir = 160000000, .cbs = 640000, .ebs = 1280000}, // flow 0
    {.cir = 80000000, .cbs = 70000, .ebs = 360000},    // flow 1
    {.cir = 40000000, .cbs = 32000, .ebs = 200000},    // flow 2
    {.cir = 20000000, .cbs = 16000, .ebs = 160000}     // flow 3
};

/* define FLOW_METER */
struct rte_meter_srtcm app_flow[APP_FLOWS_MAX];

/* define CPU_TIME_STAMP_REFERENCE */
uint64_t cpu_time_stamp_reference[APP_FLOWS_MAX];

/**
 * This function will be called only once at the beginning of the test. 
 * You can initialize your meter here.
 * 
 * int rte_meter_srtcm_config(struct rte_meter_srtcm *m, struct rte_meter_srtcm_params *params);
 * @return: 0 upon success, error code otherwise
 * 
 * void rte_exit(int exit_code, const char *format, ...)
 * #define rte_panic(...) rte_panic_(__func__, __VA_ARGS__, "dummy")
 * 
 * uint64_t rte_get_tsc_hz(void)
 * @return: The frequency of the RDTSC timer resolution
 * 
 * static inline uint64_t rte_get_tsc_cycles(void)
 * @return: The time base for this lcore.
 */
int qos_meter_init(void) /* ported from app_configure_flow_table() */
{
    uint32_t i, j;
    int ret;

    for (i = 0, j = 0; i < APP_FLOWS_MAX;
         i++, j = (j + 1) % RTE_DIM(app_srtcm_params))
    {
        cpu_time_stamp_reference[i] = rte_rdtsc(); // record cpu time stamp during initialization as a reference
        ret = rte_meter_srtcm_config(&app_flow[i], &app_srtcm_params[j]);
        if (ret)
        {
            printf("QOS_METER: Initialization failed");
            return ret;
        }
    }

    return 0;
}

/**
 * This function will be called for every packet in the test, 
 * after which the packet is marked by returning the corresponding color.
 * 
 * A packet is marked green if it doesn't exceed the CBS, 
 * yellow if it does exceed the CBS, but not the EBS, and red otherwise
 * 
 * The pkt_len is in bytes, the time is in nanoseconds.
 * 
 * Point: We need to convert ns to cpu cycles
 * Point: Time is not counted from 0
 * 
 * static inline enum rte_meter_color rte_meter_srtcm_color_blind_check(struct rte_meter_srtcm *m,
	uint64_t time, uint32_t pkt_len)
 * 
 * enum qos_color { GREEN = 0, YELLOW, RED };
 * enum rte_meter_color { e_RTE_METER_GREEN = 0, e_RTE_METER_YELLOW,  
	e_RTE_METER_RED, e_RTE_METER_COLORS };
 */
enum qos_color
qos_meter_run(uint32_t flow_id, uint32_t pkt_len, uint64_t time) /* ported from app_pkt_handle() */
{
    uint8_t output_color;                                               // since we use blind mode, input_color is ignored
    uint64_t tsc_frequency = rte_get_tsc_hz();                          // get ??? cycles per second
    uint64_t cpu_time_stamp_offset = time * tsc_frequency / 1000000000; // compute cpu time stamp offset in cycles
    output_color = (uint8_t)rte_meter_srtcm_color_blind_check(&app_flow[flow_id],
                                                              cpu_time_stamp_reference[flow_id] + cpu_time_stamp_offset,
                                                              pkt_len);
    return output_color;
}

/* define PARAMS, use random picked value for now */
struct rte_red_params app_red_params[APP_FLOWS_MAX][e_RTE_METER_COLORS] = {
    {
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // green
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // yellow
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}  // red
    },                                                                // red pararms of the colors above of flow 0
    {
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // green
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // yellow
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}  // red
    },                                                                // red pararms of the colors above of flow 1
    {
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // green
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // yellow
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}  // red
    },                                                                // red pararms of the colors above of flow 2
    {
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // green
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}, // yellow
        {.wq_log2 = 8, .min_th = 256, .max_th = 512, .maxp_inv = 16}  // red
    }                                                                 // red pararms of the colors above of flow 3
};

/* define red run-time data */
struct rte_red app_red[APP_FLOWS_MAX][e_RTE_METER_COLORS];

/* define red configs */
struct rte_red_config app_red_config[APP_FLOWS_MAX][e_RTE_METER_COLORS];

/* define queue size */
unsigned queue_size[APP_FLOWS_MAX];

/**
 * This function will be called only once at the beginning of the test. 
 * You can initialize you dropper here
 * 
 * int rte_red_rt_data_init(struct rte_red *red);
 * @return Operation status, 0 success
 * 
 * int rte_red_config_init(struct rte_red_config *red_cfg, const uint16_t wq_log2, 
   const uint16_t min_th, const uint16_t max_th, const uint16_t maxp_inv);
 * @return Operation status, 0 success 
 */
int qos_dropper_init(void)
{
    uint32_t i, j;
    int ret;

    for (i = 0; i < APP_FLOWS_MAX; i++)
    {
        queue_size[i] = 0; // queue size start from 0
        for (j = 0; j < e_RTE_METER_COLORS; j++)
        {
            ret = rte_red_rt_data_init(&app_red[i][j]);
            if (ret)
                return ret;
            ret = rte_red_config_init(&app_red_config[i][j],
                                      app_red_params[i][j].wq_log2,
                                      app_red_params[i][j].min_th,
                                      app_red_params[i][j].max_th,
                                      app_red_params[i][j].maxp_inv);
            if (ret)
            {
                printf("QOS_DROPPER: Initialization failed");
                return ret;
            }
        }
    }

    return 0;
}

/**
 * This function will be called for every tested packet after being marked by the meter, 
 * and will make the decision whether to drop the packet by returning the decision (0 pass, 1 drop)
 * 
 * The probability of drop increases as the estimated average queue size grows
 * 
 * static inline void rte_red_mark_queue_empty(struct rte_red *red, const uint64_t time)
 * @brief Callback to records time that queue became empty
 * @param q_time : Start of the queue idle time (q_time) 
 * 
 * static inline int rte_red_enqueue(const struct rte_red_config *red_cfg,
	struct rte_red *red, const unsigned q, const uint64_t time)
 * @param q [in] updated queue size in packets   
 * @return Operation status
 * @retval 0 enqueue the packet
 * @retval 1 drop the packet based on max threshold criteria
 * @retval 2 drop the packet based on mark probability criteria
 */
int qos_dropper_run(uint32_t flow_id, enum qos_color color, uint64_t time)
{
    if (time - app_red[flow_id][color].q_time > 1000)
    {
        rte_red_mark_queue_empty(&app_red[flow_id][color], time);
        uint32_t i;
        for (i = 0; i < APP_FLOWS_MAX; i++)
            queue_size[i] = 0;
    }
    int ret;
    uint64_t tsc_frequency = rte_get_tsc_hz();                          // get ??? cycles per second
    uint64_t cpu_time_stamp_offset = time * tsc_frequency / 1000000000; // compute cpu time stamp offset in cycles
    ret = rte_red_enqueue(&app_red_config[flow_id][color],
                          &app_red[flow_id][color],
                          queue_size[flow_id],
                          cpu_time_stamp_reference[flow_id] + cpu_time_stamp_offset);
    if (ret == 0)
        queue_size[flow_id]++;
    return ret;
}

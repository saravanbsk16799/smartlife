package com.stark.smartlife.aod

/**
 * Always-On Display capability of the Fastrack Jupiter R2.
 *
 * Findings (Nov 2026): published specifications describe a 1.38" TFT/LCD 240x240 panel with
 * IP68, 5-day battery, SingleSync BT calling and a functional crown, and none list an
 * Always-On Display. Fastrack's own range lists "always-on display" only on AMOLED models
 * (e.g. Xtreme Pro). No AOD command exists in any documentation we could verify.
 *
 * Therefore this app offers no ON/OFF, schedule, clock-style or brightness controls and sends
 * nothing to the watch. If a future firmware or verified protocol adds AOD, implement it here.
 */
object AodCapability {
    val supported: Boolean = false
}

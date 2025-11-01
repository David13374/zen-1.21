@file:Suppress("UNUSED")

package xyz.meowing.zen.events.core

import net.minecraft.network.packet.Packet
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event

abstract class PacketEvent {
    class Received(
        val packet: Packet<*>
    ) : CancellableEvent()

    class ReceivedPost(
        val packet: Packet<*>
    ) : Event()

    class Sent(
        val packet: Packet<*>
    ) : Event()

    class SentPost(
        val packet: Packet<*>
    ) : Event()
}
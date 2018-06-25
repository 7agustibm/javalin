package io.javalin.serversentevent

import org.junit.Test
import org.mockito.Mockito.*

class EventSourceImplTest {
    val event = "event"
    val data = "data"

    @Test
    fun onOpen() {

        val emitter = mock(Emitter::class.java)
        val onOpen = mock(SSEConnect::class.java)

        val eventSource = EventSourceImpl(emitter)
        eventSource.onOpen(onOpen)

        verify(onOpen).handler(eventSource)
    }


    @Test
    fun sendEvent() {
        val emitter = mock(Emitter::class.java)
        `when`(emitter.isClose()).thenReturn(false)
        val eventSource = EventSourceImpl(emitter)
        eventSource.sendEvent(event, data)

        verify(emitter).event(event, data)
    }

    @Test
    fun onClose() {

        val emitter = mock(Emitter::class.java)
        val onClose = mock(SSEClose::class.java)
       `when`(emitter.isClose()).thenReturn(true)
        val eventSource = EventSourceImpl(emitter)
        eventSource.onClose(onClose)

        eventSource.sendEvent(event, data)
        verify(onClose).handler(eventSource)
        verify(onClose).handler(eventSource)
    }

}

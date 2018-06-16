/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.core.JavalinServlet
import io.javalin.websocket.JavalinWsRouter
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.net.BindException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object JettyServerUtil {

    @JvmStatic
    @Throws(BindException::class)
    fun initialize(
            server: Server,
            port: Int,
            contextPath: String,
            javalinServlet: JavalinServlet,
            javalinWsRouter: JavalinWsRouter,
            log: Logger
    ): Int {

        val parent = null // javalin handlers are orphans

        val httpHandler = object : ServletContextHandler(parent, contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                try {
                    request.setAttribute("jetty-target", target)
                    request.setAttribute("jetty-request", jettyRequest)
                    javalinServlet.service(request, response)
                } catch (e: Exception) {
                    response.status = 500
                    log.error("Exception occurred while servicing http-request", e)
                }
                jettyRequest.isHandled = true
            }
        }

        val webSocketHandler = ServletContextHandler(parent, contextPath).apply {
            addServlet(ServletHolder(object : WebSocketServlet() {
                override fun configure(factory: WebSocketServletFactory) {
                    factory.creator = WebSocketCreator { req, res ->
                        javalinWsRouter.findEntry(req) ?: res.sendError(404, "WebSocket handler not found")
                        javalinWsRouter // this is a long-lived object handling multiple connections
                    }
                }
            }), "/*")
        }

        val notFoundHandler = object : SessionHandler() {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                val msg = "Not found. Request is below context-path (context-path: '${contextPath}')"
                response.status = 404
                ByteArrayInputStream(msg.toByteArray()).copyTo(response.outputStream)
                response.outputStream.close()
                log.warn("Received a request below context-path (context-path: '${contextPath}'). Returned 404.")
            }
        }

        server.apply {
            handler = attachHandlersToTail(server.handler, HandlerList(httpHandler, webSocketHandler, notFoundHandler))
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = port
            })
        }.start()

        log.info("Jetty is listening on: " + server.connectors.map { (if (it.protocols.contains("ssl")) "https" else "http") + "://localhost:" + (it as ServerConnector).localPort })

        return (server.connectors[0] as ServerConnector).localPort
    }

    private fun attachHandlersToTail(userHandler: Handler?, handlerList: HandlerList): HandlerWrapper {
        val handlerWrapper = (userHandler ?: HandlerWrapper()) as HandlerWrapper
        HandlerWrapper().apply { handler = handlerList }.insertHandler(handlerWrapper)
        return handlerWrapper
    }

    private fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader("Sec-WebSocket-Key") != null
}

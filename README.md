# webflux-websocket

The WebSocket protocol, RFC 6455, provides a standardized way to establish a full-duplex, two-way communication channel between client and server over a single TCP connection. It is a different TCP protocol from HTTP but is designed to work over HTTP, using ports 80 and 443 and allowing re-use of existing firewall rules


**When to Use WebSockets?**

WebSockets can make a web page be dynamic and interactive. However, in many cases, a combination of AJAX and HTTP streaming or long polling can provide a simple and effective solution.

Note: Keep in mind also that over the Internet, restrictive proxies that are outside of your control may preclude WebSocket interactions, either because they are not configured to pass on the Upgrade header or because they close long-lived connections that appear idle. This means that the use of WebSocket for internal applications within the firewall is a more straightforward decision than it is for public facing applications.

reference documents:
-[webflux-websocket spring 6.0.x](https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html)

In this project, we will create a simple chat application using Spring WebFlux and WebSocket. which will cover:

1. Brodcast message to all connected clients
2. Brodcast message to a room
3. Brodcast message to a specific client
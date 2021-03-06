package com.aphyr.riemann.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Arrays;

import com.aphyr.riemann.Proto.Event;
import com.aphyr.riemann.Proto.Query;
import com.aphyr.riemann.Proto.Msg;
import com.aphyr.riemann.client.ServerError;


public abstract class RiemannClient {

  public static final int DEFAULT_PORT = 5555;

  protected final InetSocketAddress server;

  public RiemannClient(final InetSocketAddress server) {
    this.server = server;
  }

  public RiemannClient(final int port) throws UnknownHostException {
    this.server = new InetSocketAddress(InetAddress.getLocalHost(), port);
  }

  public RiemannClient() throws UnknownHostException {
    this(new InetSocketAddress(InetAddress.getLocalHost(), DEFAULT_PORT));
  }
 
  public EventDSL event() {
    return new EventDSL(this);
  }

  // Sends events and checks the server's response. Will throw IOException for
  // network failures, ServerError for error responses from Riemann. Returns
  // true if events acknowledged.
  public Boolean sendEventsWithAck(final Event... events) throws IOException, ServerError {
    validate(
        sendRecvMessage(
          Msg.newBuilder()
            .addAllEvents(Arrays.asList(events))
            .build()
        )
    );
    return true;
  }

  // Sends events in fire-and-forget fashion. Doesn't check server response,
  // swallows all exceptions silently. No guarantees on delivery.
  public void sendEvents(final Event... events) {
    try {
      sendMaybeRecvMessage(
         Msg.newBuilder()
          .addAllEvents(Arrays.asList(events))
          .build()
      );
    } catch (IOException e) {
      // Fuck it.
    }
  }

  public List<Event> query(String q) throws IOException, ServerError {
    Msg m = sendRecvMessage(Msg.newBuilder()
        .setQuery(
          Query.newBuilder().setString(q).build())
        .build());

    validate(m);

    return m.getEventsList();
  }

  public abstract void sendMessage(Msg message) throws IOException;

  public abstract Msg recvMessage() throws IOException;

  public abstract Msg sendRecvMessage(Msg message) throws IOException;

  public abstract Msg sendMaybeRecvMessage(Msg message) throws IOException;

  public abstract boolean isConnected();

  public abstract void connect() throws IOException;

  public abstract void disconnect() throws IOException;

  // Asserts that the message is OK; if not, throws a ServerError.
  public Msg validate(Msg message) throws IOException, ServerError {
    if (message.hasOk() && message.getOk() == false) {
      throw(new ServerError(message.getError()));
    }
    return message;
  } 
}

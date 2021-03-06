/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package docs.actor;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import akka.testkit.JavaTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.Await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InitializationDocTest {

  static ActorSystem system = null;

  @BeforeClass
  public static void beforeClass() {
    system = ActorSystem.create("InitializationDocTest");
  }

  @AfterClass
  public static void afterClass() throws Exception {
      Await.ready(system.terminate(), Duration.create("5 seconds"));
  }

  public static class MessageInitExample extends AbstractActor {
    private String initializeMe = null;

    public MessageInitExample() {
      //#messageInit
      receive(ReceiveBuilder.
        matchEquals("init", m1 -> {
          initializeMe = "Up and running";
          context().become(ReceiveBuilder.
            matchEquals("U OK?", m2 -> {
              sender().tell(initializeMe, self());
            }).build());
        }).build()
      //#messageInit
      );
    }
  }

  @Test
  public void testIt() {

    new JavaTestKit(system) {{
      ActorRef testactor = system.actorOf(Props.create(MessageInitExample.class), "testactor");
      String msg = "U OK?";

      testactor.tell(msg, getRef());
      expectNoMsg(Duration.create(1, TimeUnit.SECONDS));

      testactor.tell("init", getRef());
      testactor.tell(msg, getRef());
      expectMsgEquals("Up and running");
    }};
  }
}

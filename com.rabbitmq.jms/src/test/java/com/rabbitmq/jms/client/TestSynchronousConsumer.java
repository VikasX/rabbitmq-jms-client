package com.rabbitmq.jms.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

public class TestSynchronousConsumer {

    private static final long TIMEOUT = 100;
    private static final Envelope envelope = mock(Envelope.class);
    static {
        when(envelope.getDeliveryTag()).thenReturn(1l);
    }
    private static final GetResponse TEST_RESPONSE = new GetResponse(envelope, null, null, 0);

    /**
     * This test the message is exchanged successfully
     * @throws Exception
     */
    @Test
    public void testSynchronousConsumerSuccess() throws Exception {
        Channel channel = mock(Channel.class);

        SynchronousConsumer consumer = new SynchronousConsumer(channel, TIMEOUT);
        CountDownLatch tx = new CountDownLatch(1);
        CountDownLatch rx = new CountDownLatch(1);
        SenderThread st = new SenderThread(TEST_RESPONSE, consumer, tx);
        ReceiverThread rt = new ReceiverThread(TEST_RESPONSE, consumer, rx);
        st.start();
        rt.start();

        rx.countDown();
        tx.countDown();
        
        rt.join();
        st.join();
        
        verify(channel, atLeastOnce()).basicAck(anyLong(),anyBoolean());
        verify(channel, atLeastOnce()).basicCancel(anyString());
        assertTrue(rt.isSuccess());
        assertTrue(st.isSuccess());
    }
    
    @Test
    public void testSynchronousConsumerSuccessShortTimeout() throws Exception {
        Channel channel = mock(Channel.class);

        SynchronousConsumer consumer = new SynchronousConsumer(channel, 1);
        CountDownLatch tx = new CountDownLatch(1);
        CountDownLatch rx = new CountDownLatch(1);
        SenderThread st = new SenderThread(TEST_RESPONSE, consumer, tx);
        ReceiverThread rt = new ReceiverThread(TEST_RESPONSE, consumer, rx);
        st.start();
        rt.start();

        rx.countDown();
        tx.countDown();
        
        rt.join();
        st.join();
        
        verify(channel, atLeastOnce()).basicAck(anyLong(),anyBoolean());
        verify(channel, atLeastOnce()).basicCancel(anyString());
        assertTrue(rt.isSuccess());
        assertTrue(st.isSuccess());
    }
    
    /**
     * In this test the receiver should timeout, since there
     * is a delay in sending, and the sender must NACK the message
     * @throws Exception
     */
    @Test
    public void testSynchronousConsumerReceiverTimeout() throws Exception {
        Channel channel = mock(Channel.class);

        SynchronousConsumer consumer = new SynchronousConsumer(channel, TIMEOUT);
        CountDownLatch tx = new CountDownLatch(1);
        CountDownLatch rx = new CountDownLatch(1);
        SenderThread st = new SenderThread(TEST_RESPONSE, consumer, tx);
        ReceiverThread rt = new ReceiverThread(TEST_RESPONSE, consumer, rx);
        st.start();
        rt.start();

        rx.countDown();
        Thread.sleep(2*TIMEOUT);
        tx.countDown();
        
        rt.join();
        st.join();
        
        
        verify(channel, atLeastOnce()).basicNack(anyLong(),anyBoolean(), anyBoolean());
        verify(channel, atLeastOnce()).basicCancel(anyString());
        assertFalse(rt.isSuccess());
        assertTrue(st.isSuccess());
    }
    
    /**
     * In this test the receiver should timeout, since there
     * is a no sending at all
     * @throws Exception
     */
    @Test
    public void testSynchronousConsumerReceiverTimeoutNoSender() throws Exception {
        Channel channel = mock(Channel.class);

        SynchronousConsumer consumer = new SynchronousConsumer(channel, TIMEOUT);
        CountDownLatch rx = new CountDownLatch(1);
        ReceiverThread rt = new ReceiverThread(TEST_RESPONSE, consumer, rx);
        rt.start();

        rx.countDown();
        
        rt.join();
        
        assertFalse(rt.isSuccess());
    }
    

    private static class SenderThread extends Thread {
        final GetResponse response;
        final SynchronousConsumer consumer;
        final CountDownLatch latch;
        volatile boolean success = false;
        volatile Exception exception;

        public SenderThread(GetResponse response, SynchronousConsumer consumer, CountDownLatch latch) {
            super();
            this.response = response;
            this.consumer = consumer;
            this.latch = latch;
        }

        public void run() {
            final String fakeConsumerTag = "";
            try {
                latch.await();
                consumer.handleDelivery(fakeConsumerTag, response);
                success = true;
            } catch (Exception x) {
                x.printStackTrace(); //TODO logging implementation
                exception = x;
                success = false;
                return;
            }
            try {
                latch.await();
                consumer.handleDelivery(fakeConsumerTag, response);
                success = false;
            } catch (Exception x) {
                //this is expected, it's a 2nd invocation
                exception = x;
                success = true;
                return;
            }
        }

        public boolean isSuccess() {
            return success;
        }

        @SuppressWarnings("unused")
        public Exception getException() {
            return exception;
        }
    }

    private static class ReceiverThread extends Thread {
        final GetResponse response;
        final SynchronousConsumer consumer;
        final CountDownLatch latch;
        volatile boolean success = false;
        volatile Exception exception;

        public ReceiverThread(GetResponse response, SynchronousConsumer consumer, CountDownLatch latch) {
            this.response = response;
            this.consumer = consumer;
            this.latch = latch;
        }

        public void run() {
            try {
                latch.await();
                success = (consumer.receive() == response);
            } catch (Exception x) {
                exception = x;
                success = false;
                return;
            }
        }

        public boolean isSuccess() {
            return success;
        }

        @SuppressWarnings("unused")
        public Exception getException() {
            return exception;
        }
    }

    public static void printContentsOfStackTrace(final StackTraceElement[] stackTrace, final OutputStream out) {
        int index = 0;
        String newLine = "\n";
        for (StackTraceElement element : stackTrace) {
            try {
                out.write(newLine.getBytes());
                out.write(("Index: " + index++ + newLine).getBytes());
                out.write(("ClassName: " + element.getClassName() + newLine).getBytes());
                out.write(("MethodName: " + element.getMethodName() + newLine).getBytes());
                out.write(("FileName: " + element.getFileName() + newLine).getBytes());
                out.write(("LineNumber: " + element.getLineNumber() + newLine).getBytes());
            } catch (IOException ioEx) {
                System.err.println("IOException trying to write out contents of StackTraceElement[]:\n" + ioEx.getMessage());
            }
        }
    }

}
package com.kalouantonis.channel.concurrent

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import scala.concurrent.blocking

/** Represents a single-item queue.
  *
  * This is a thread-safe queue, allowing both blocking and non-blocking
  * calls for both the reader and writer.
  *
  * Unlike scala.concurrent.Channel, this class has methods for both blocking
  * and non-blocking calls.
  */
class Channel[T] {
  private[this] val queue: LinkedBlockingQueue[T] =
    new LinkedBlockingQueue(1)

  // TODO: find out if its ok to declare blocking here

  /** Retrieve an item, removing it from the queue.
    *
    * This is a blocking operation, it will wait until an item is placed
    * on the queue.
    */
  def get: T = blocking(queue.take())

  /** Return an item in the queue, if it exists, without removing it
    * from the queue.
    *
    * @return the current item in the queue, if available.
    */
  def peek: Option[T] = Option(queue.peek())

  /** Retrieve an item from the queue, if it exists, and remove it from
    * the queue.
    *
    * This is a non-blocking operation, it will either return an item, removing
    * it from the queue or it will return nothing.
    *
    * @return the current item
    */
  def poll: Option[T] = Option(queue.poll())

  /** Place an item on to the queue, blocking if the queue is full.
    *
    * @param item the item to place on to the queue.
    */
  def put(item: T): Unit = blocking(queue.put(item))

  /** Attempt to place an item on the queue.
    *
    * This attempts to place an item on the queue in a non-blocking fashion,
    * returning true if it was emplaced correctly or false if the queue is
    * already considered full.
    *
    * @param item the item to place on the queue
    * @return true if the item was placed in the queue
    */
  def offer(item: T): Boolean = queue.offer(item)

  /** Return true if the queue contains an item. */
  def isDefined: Boolean = peek.isDefined

  /** Return true if the queue contains is empty. */
  def isEmpty: Boolean = peek.isEmpty
}

case class DuplexChannel[In, Out](in: Channel[In], out: Channel[Out])

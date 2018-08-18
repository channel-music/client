package com.kalouantonis.channel.concurrent

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

/** Represents a single-item queue.
  *
  * This is a thread-safe queue, allowing both blocking and non-blocking
  * calls for both the reader and writer.
  */
class Channel[T] {
  private[this] val queue: LinkedBlockingQueue[T] =
    new LinkedBlockingQueue(1)

  /** Retrieve an item, removing it from the queue.
    *
    * This is a blocking operation, it will wait until an item is placed
    * on the queue.
    */
  def get: T = queue.take()

  /** Return an item in the queue, if it exists, without removing it
    * from the queue.
    *
    * @return the current item in the queue, if available.
    */
  def peek: Option[T] = Option(queue.peek())

  /** Retrieve an item from the queue, if it exists and remove it from
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
  def put(item: T): Unit = queue.put(item)

  /** Return true if the queue contains an item. */
  def isDefined: Boolean = peek.isDefined

  /** Return true if the queue contains is empty. */
  def isEmpty: Boolean = peek.isEmpty
}

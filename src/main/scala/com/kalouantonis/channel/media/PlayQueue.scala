package kalouantonis.channel.media

/**
  * An immutable PlayQueue data structure.
  */
class PlayQueue[T] private(items: IndexedSeq[T], index: Int) {
  /**
    * Add an item to the end of the queue.
    */
  def append(item: T): PlayQueue[T] =
    // is :+ efficient for IndexedSeq?
    new PlayQueue(items :+ item, index)

  /**
    * Return a queue with the play index reset to the beginning.
    */
  def reset: PlayQueue[T] =
    new PlayQueue(items, 0)

  /**
    * Return the current item in the play queue, if there is one.
    */
  def current: Option[T] = items.lift(index)

  /**
    * Return a queue with the index shifted one item to the right.
    */
  def next: PlayQueue[T] =
    new PlayQueue(items, index + 1)

  /**
    * Return a queue with the index shifted one item to the left.
    */
  def previous: PlayQueue[T] =
    // FIXME: how are we gonna handle OOB in scala?
    new PlayQueue(items, if (index == 0) index else index - 1)

  /**
    * Return true if the queue contains no items.
    */
  def isEmpty: Boolean = items.isEmpty
}

object PlayQueue {
  def apply[T](items: IndexedSeq[T]): PlayQueue[T] =
    new PlayQueue(items, 0)

  def empty[T]: PlayQueue[T] = PlayQueue(IndexedSeq())
}

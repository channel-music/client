package com.kalouantonis.channel.media

import org.scalatest._

class PlayQueueTest extends FunSpec with Matchers {
  describe("current") {
    it("returns the current item in the queue when it is available") {
      val pq = PlayQueue(Vector(1, 2, 3))
      pq.current shouldBe Some(1)
    }

    it("returns nothing if there is no item available") {
      val pq = PlayQueue(Vector())
      pq.current shouldBe None
    }
  }
}

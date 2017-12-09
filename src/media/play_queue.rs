use rand;
use rand::Rng;

// TODO: ensure this does what I expect
#[derive(Clone)]
pub struct PlayQueue<T> {
    items: Vec<T>,
    index: usize,
}

// TODO: consider implementing as immutable
impl<T: PartialEq + Clone> PlayQueue<T> {
    /// Create a new empty play queue.
    pub fn new() -> PlayQueue<T> {
        PlayQueue::from_vec(vec![])
    }

    /// Create a new play queue using the items from vector `items`.
    pub fn from_vec(items: Vec<T>) -> PlayQueue<T> {
        PlayQueue {
            items: items,
            index: 0,
        }
    }

    /// Add an item to the end of the queue.
    pub fn append(&mut self, item: &T) {
        self.items.push(item.clone());
    }

    /// Remove all items from the queue.
    pub fn clear(&mut self) {
        self.items = vec![];
        self.index = 0;
    }

    /// Reset the queue back to the first item.
    pub fn reset(&mut self) {
        self.index = 0;
    }

    /// Move to the next item in the queue, returning it.
    ///
    /// Returns `None` if there is no next item in the queue.
    pub fn next(&mut self) -> Option<&T> {
        let next_item = self.items.get(self.index + 1);

        if next_item.is_some() {
            self.index += 1;
        }

        next_item
    }

    /// Move to the previous item in the queue, returning it.
    ///
    /// Returns `None` if there is no previous item in the queue.
    pub fn previous(&mut self) -> Option<&T> {
        let previous_item = match self.index.checked_sub(1) {
            Some(index) => self.items.get(index),
            None => None,
        };

        if previous_item.is_some() {
            self.index -= 1;
        }

        previous_item
    }

    /// Return the current item in the queue. Returns `None` if there aren't
    /// any items. This usually occurs when the play queue is empty.
    pub fn current(&self) -> Option<&T> {
        self.items.get(self.index)
    }

    /// Jump to an item in the queue. Will panic if it does not exist.
    pub fn jump_to(&mut self, item: &T) {
        self.index = self.items.iter().position(|v| v == item).unwrap();
    }

    /// Shuffle tracks in the queue, keeping the current item at the front.
    pub fn shuffle(&mut self) {
        let mut shuffled_items = self.items.clone();
        // Remove current item and add it later so that the
        // current item is always furst.
        shuffled_items.remove(self.index);
        rand::thread_rng().shuffle(&mut shuffled_items);

        self.items = match self.current() {
            Some(v) => {
                let mut items = vec![v.clone()];
                items.append(&mut shuffled_items);
                items
            },
            None => shuffled_items
        };
        self.index = 0;
    }
}


#[cfg(test)]
mod tests {
    use super::PlayQueue;

    #[test]
    fn test_current_none_when_empty() {
        let q: PlayQueue<u8> = PlayQueue::new();
        assert!(q.current().is_none());
    }

    #[test]
    fn test_current_when_not_empty() {
        let q = PlayQueue::from_vec(vec![1, 2, 3]);
        assert_eq!(q.current().unwrap(), &1);
    }

    #[test]
    fn  test_append_adds_new_items() {
        let mut q = PlayQueue::new();
        q.append(&1);
        assert_eq!(q.current().unwrap(), &1);
        q.append(&2);
        assert_eq!(q.current().unwrap(), &1);
    }

    #[test]
    fn test_clear_removes_all_items() {
        let mut q = PlayQueue::from_vec(vec![1, 2, 3]);
        assert!(q.current().is_some());
        q.clear();
        assert!(q.current().is_none());
    }

    #[test]
    fn test_next_moves_to_next_item() {
        let mut q = PlayQueue::from_vec(vec![1, 2]);
        assert_eq!(q.current().unwrap(), &1);
        assert_eq!(q.next().unwrap(), &2);
        assert_eq!(q.current().unwrap(), &2);
        assert!(q.next().is_none());
        assert_eq!(q.current().unwrap(), &2);
    }

    #[test]
    fn test_next_is_none_with_no_items() {
        let mut q: PlayQueue<u8> = PlayQueue::new();
        assert!(q.next().is_none());
    }

    #[test]
    fn test_previous_moves_to_previous_item() {
        let mut q = PlayQueue::from_vec(vec![1, 2]);
        q.next().unwrap();
        assert_eq!(q.current().unwrap(), &2);
        assert_eq!(q.previous().unwrap(), &1);
        assert!(q.previous().is_none());
        assert_eq!(q.current().unwrap(), &1);
    }

    #[test]
    fn test_previous_is_none_with_no_items() {
        let mut q: PlayQueue<u8> = PlayQueue::new();
        assert!(q.previous().is_none());
    }

    #[test]
    fn test_reset_moves_to_beginning() {
        let mut q = PlayQueue::from_vec(vec![1, 2, 3, 4, 5]);
        assert_eq!(q.next().unwrap(), &2);
        assert_eq!(q.next().unwrap(), &3);
        q.reset();
        assert_eq!(q.current().unwrap(), &1);
    }

    #[test]
    fn test_jump_to_valid_item() {
        let mut q = PlayQueue::from_vec((1..15).collect());
        for i in 1..15 {
            q.append(&i);
        }

        q.jump_to(&7);
        assert_eq!(q.current().unwrap(), &7);
    }

    #[test]
    #[should_panic]
    fn test_jump_to_missing_item() {
        let mut q: PlayQueue<u8> = PlayQueue::new();
        q.jump_to(&18);
    }

    #[test]
    fn test_shuffle() {
        let mut q = PlayQueue::new();
        for i in 1..10000 {
            q.append(&i);
        }
        q.shuffle();
        // First item is the same
        assert_eq!(q.current().unwrap(), &1);
        // There's a tiny chance this fails, just run again
        assert_ne!(q.next().unwrap(), &2);
    }
}

using System;
using System.Collections.Generic;
using System.Threading;
using clojure.core.async;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace PriorityQueueTests
{
    [TestClass()]
    public class DelayQueueTest
    {
        public static DateTime TestNow;

        public class DelayItem : IDelayed
        {
            DateTime _due;

            public DelayItem(DateTime due)
            {
                _due = due;
            }

            public TimeSpan GetRemainingDelay()
            {
                return _due-TestNow;
            }

            public int CompareTo(IDelayed other)
            {
                return GetRemainingDelay().CompareTo(other.GetRemainingDelay());
            }
        }


        /// <summary>
        ///A test for DelayQueue Constructor
        ///</summary>
        [TestMethod()]
        public void DelayQueueConstructorTest()
        {
            DelayQueue target = new DelayQueue();
            Assert.AreEqual(0, target.Count);
        }

        [TestMethod()]
        public void DelayQueuePeekNothingTest()
        {
            DelayQueue target = new DelayQueue();
            Assert.IsNull(target.poll());
            Assert.IsNull(target.peek());
        }

        [TestMethod()]
        public void DelayQueuePutSome()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000,1,1);
            TimeSpan delta = new TimeSpan(1,0,0,0);
            target.put(new DelayItem(TestNow+delta));
            Assert.AreEqual(1,target.Count);
            target.put(new DelayItem(TestNow+delta+delta));
            Assert.AreEqual(2,target.Count);
        }

        [TestMethod()]
        public void DelayQueuePeekNotReady()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000, 1, 1);
            TimeSpan delta = new TimeSpan(1, 0, 0, 0);
            DelayItem item1 = new DelayItem(TestNow + delta);
            DelayItem item2 = new DelayItem(TestNow + delta + delta);
            target.put(item1);
            target.put(item2);
            Assert.IsNull(target.peek()); 
            Assert.AreEqual(2, target.Count);
        }

        [TestMethod()]
        public void DelayQueuePollNotReady()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000, 1, 1);
            TimeSpan delta = new TimeSpan(1, 0, 0, 0);
            DelayItem item1 = new DelayItem(TestNow + delta);
            DelayItem item2 = new DelayItem(TestNow + delta + delta);
            target.put(item1);
            target.put(item2);
            Assert.IsNull(target.poll());
            Assert.AreEqual(2, target.Count);
        }

        [TestMethod()]
        public void DelayQueuePeekReady()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000, 1, 1);
            TimeSpan delta = new TimeSpan(1, 0, 0, 0);
            DelayItem item1 = new DelayItem(TestNow + delta);
            DelayItem item2 = new DelayItem(TestNow - delta);
            target.put(item1);
            target.put(item2);
            Assert.AreEqual(item2,target.peek());
            Assert.AreEqual(2, target.Count);
        }

        [TestMethod()]
        public void DelayQueuePollReady()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000, 1, 1);
            TimeSpan delta = new TimeSpan(1, 0, 0, 0);
            DelayItem item1 = new DelayItem(TestNow + delta);
            DelayItem item2 = new DelayItem(TestNow - delta);
            target.put(item1);
            target.put(item2);
            Assert.AreEqual(item2, target.poll());
            Assert.AreEqual(1, target.Count);
        }

        [TestMethod()]
        public void DelayQueuePollMultipleReady()
        {
            DelayQueue target = new DelayQueue();
            TestNow = new DateTime(2000, 1, 1);
            TimeSpan delta = new TimeSpan(1, 0, 0, 0);
            const int numItems = 10;

            SortedList<TimeSpan,DelayItem> items = new SortedList<TimeSpan,DelayItem>(10);

            // Put in five ready items, five not ready items
            {
                TimeSpan d2 = delta;
                for (int i = 0; i < numItems/2; i++, d2 += delta)
                {
                    DelayItem iplus = new DelayItem(TestNow + d2);
                    DelayItem iminus = new DelayItem(TestNow - d2);
                    target.put(iplus);
                    target.put(iminus);
                    items.Add(d2,iplus);
                    items.Add(-d2,iminus);
                }
            }

            Assert.AreEqual(numItems, target.Count);

            for (int i = 0; i < numItems / 2; i++)
            {
                object qItem = target.poll();
                object item = items[items.Keys[0]];
                items.RemoveAt(0);
                Assert.AreSame(item, qItem);
                Assert.AreEqual(items.Count, target.Count);
            }

            Assert.IsNull(target.peek());
            Assert.IsNull(target.poll());
        }

        [TestMethod()]
        public void DelayQueuePollInOrder()
        {
            List<DelayItem> takenItems = new List<DelayItem>();
            SortedList<DateTime, DelayItem> items = new SortedList<DateTime, DelayItem>();

            DelayQueue target = new DelayQueue();

            TestNow = new DateTime(2000, 1, 1);
            List<int> offsets = new List<int>();
            offsets.Add(8);
            offsets.Add(6);
            offsets.Add(7);
            offsets.Add(5);

            foreach (int i in offsets)
            {
                DateTime dt = TestNow + new TimeSpan(0, 0, i);
                DelayItem item = new DelayItem(dt);
                items.Add(dt, item);
                target.put(item);
            }

            TestNow += new TimeSpan(0, 0, 50);
            while (target.Count > 0)
                takenItems.Add((DelayItem)target.poll());


            Assert.AreEqual(items.Count, takenItems.Count);
            Assert.AreEqual(0, target.Count);

            for (int i = 0; i < items.Count; i++)
                Assert.AreSame(items[items.Keys[i]], takenItems[i]);

        }



        [TestMethod()]
        public void DelayQueueTakeInOrder()
        {
            List<DelayItem> takenItems = new List<DelayItem>();
            SortedList<DateTime, DelayItem> items = new SortedList<DateTime, DelayItem>();

            DelayQueue target = new DelayQueue();

            TestNow = new DateTime(2000, 1, 1);
            List<int> offsets = new List<int>();
            offsets.Add(8);
            offsets.Add(6);
            offsets.Add(7);
            offsets.Add(5);

            foreach (int i in offsets)
            {
                DateTime dt = TestNow + new TimeSpan(0,0,i);
                DelayItem item = new DelayItem(dt);
                items.Add(dt,item);
                target.put(item);
            }

            Thread thr = new Thread(() =>
            {
                while (target.Count > 0)

                    takenItems.Add((DelayItem)target.take());
            }); 
            
            thr.Start();

            for (int i = 0; i < 12; i++)
            {
                Thread.Sleep(1000);
                TestNow += new TimeSpan(0, 0, 1);
            }

            thr.Join();

            Assert.AreEqual(items.Count,takenItems.Count);
            Assert.AreEqual(0, target.Count);

            for (int i = 0; i < items.Count; i++)
                Assert.AreSame(items[items.Keys[i]], takenItems[i]);

        }

    }
}

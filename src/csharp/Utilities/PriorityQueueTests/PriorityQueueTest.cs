/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 *   Author: David Miller
 **/

using System;
using System.Collections.Generic;
using clojure.core.async;
using Microsoft.VisualStudio.TestTools.UnitTesting;


namespace TestPriorityQueue
{
    using ItemQueue = PriorityQueue<Item>;


    public class Item : IComparable<Item>
    {
        public int Value { get; set; }

        public Item(int value)
        {
            Value = value;
        }

        public int CompareTo(Item other)
        {
            return Value.CompareTo(other.Value);
        }
    }

    public class RevComp<T> : IComparer<T> where T : IComparable<T>
    {
        public int Compare(T x, T y)
        {
            return y.CompareTo(x);
        }
    }



    /// <summary>
    ///This is a test class for PriorityQueueTest and is intended
    ///to contain all PriorityQueueTest Unit Tests
    ///</summary>
    [TestClass()]
    public class PriorityQueueTest
    {

        private TestContext testContextInstance;

        /// <summary>
        ///Gets or sets the test context which provides
        ///information about and functionality for the current test run.
        ///</summary>
        public TestContext TestContext
        {
            get
            {
                return testContextInstance;
            }
            set
            {
                testContextInstance = value;
            }
        }

        #region Additional test attributes
        // 
        //You can use the following additional attributes as you write your tests:
        //
        //Use ClassInitialize to run code before running the first test in the class
        //[ClassInitialize()]
        //public static void MyClassInitialize(TestContext testContext)
        //{
        //}
        //
        //Use ClassCleanup to run code after all tests in a class have run
        //[ClassCleanup()]
        //public static void MyClassCleanup()
        //{
        //}
        //
        //Use TestInitialize to run code before running each test
        //[TestInitialize()]
        //public void MyTestInitialize()
        //{
        //}
        //
        //Use TestCleanup to run code after each test has run
        //[TestCleanup()]
        //public void MyTestCleanup()
        //{
        //}
        //
        #endregion



        /// <summary>
        ///A test for PriorityQueue`1 Constructor
        ///</summary>
        public void PriorityQueueConstructorTestHelper<T>()
            where T : IComparable<T>
        {
            PriorityQueue<T> target = new PriorityQueue<T>();
            Assert.AreEqual(0, target.Count);
        }

        [TestMethod()]
        public void PriorityQueueConstructorTest()
        {
            PriorityQueueConstructorTestHelper<Item>();
        }

        /// <summary>
        ///A test for PriorityQueue`1 Constructor
        ///</summary>
        public void PriorityQueueConstructorTest1Helper<T>()
            where T : IComparable<T>
        {
            int initialCapacity = 20; // TODO: Initialize to an appropriate value
            PriorityQueue<T> target = new PriorityQueue<T>(initialCapacity); 
            Assert.AreEqual(0, target.Count);
        }

        [TestMethod()]
        public void PriorityQueueConstructorTest1()
        {
            PriorityQueueConstructorTest1Helper<Item>();
        }

        /// <summary>
        ///A test for PriorityQueue`1 Constructor
        ///</summary>
        public void PriorityQueueConstructorTest2Helper<T>()
            where T : IComparable<T>
        {
            int initialCapacity = 11;
            IComparer<T> comparator = new RevComp<T>();
            PriorityQueue<T> target = new PriorityQueue<T>(initialCapacity, comparator);
            Assert.AreEqual(0, target.Count);
        }

        [TestMethod()]
        public void PriorityQueueConstructorTest2()
        {
            PriorityQueueConstructorTest2Helper<Item>();
        }

        ///// <summary>
        /////A test for Down
        /////</summary>
        //public void DownTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    int i = 0; // TODO: Initialize to an appropriate value
        //    int n = 0; // TODO: Initialize to an appropriate value
        //    target.Down(i, n);
        //    Assert.Inconclusive("A method that does not return a value cannot be verified.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void DownTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call DownTestHelper<T>() with appropriate type parameters.");
        //}

        ///// <summary>
        /////A test for Heapify
        /////</summary>
        //public void HeapifyTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    target.Heapify();
        //    Assert.Inconclusive("A method that does not return a value cannot be verified.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void HeapifyTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call HeapifyTestHelper<T>() with appropriate type parameters.");
        //}

        ///// <summary>
        /////A test for Less
        /////</summary>
        //public void LessTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    int i = 0; // TODO: Initialize to an appropriate value
        //    int j = 0; // TODO: Initialize to an appropriate value
        //    bool expected = false; // TODO: Initialize to an appropriate value
        //    bool actual;
        //    actual = target.Less(i, j);
        //    Assert.AreEqual(expected, actual);
        //    Assert.Inconclusive("Verify the correctness of this test method.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void LessTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call LessTestHelper<T>() with appropriate type parameters.");
        //}

        ///// <summary>
        /////A test for MaybeGrow
        /////</summary>
        //public void MaybeGrowTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    int minCap = 0; // TODO: Initialize to an appropriate value
        //    target.MaybeGrow(minCap);
        //    Assert.Inconclusive("A method that does not return a value cannot be verified.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void MaybeGrowTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call MaybeGrowTestHelper<T>() with appropriate type parameters.");
        //}

        public void AddItems(ItemQueue q, int[] values)
        {
            foreach (int value in values)
                q.Push(new Item(value));
        }

        public ItemQueue CreateQueue(int[] values)
        {
            ItemQueue q = new ItemQueue();
            AddItems(q, values);
            return q;               
        }

        public ItemQueue CreateReversedQueue(int[] values)
        {
            ItemQueue q = new ItemQueue(3,new RevComp<Item>());
            AddItems(q, values);
            return q;
        }


        /// <summary>
        ///A test for Peek
        ///</summary>
        [TestMethod()]
        public void PeekTest()
        {
            ItemQueue q = CreateQueue(new int[] { 4, 2, 3, 1 });
            Assert.AreEqual(1, q.Peek().Value);

            ItemQueue r = CreateReversedQueue(new int[] { 4, 2, 3, 1 });
            Assert.AreEqual(4, r.Peek().Value);

            ItemQueue e = new ItemQueue();
            Assert.IsNull(e.Peek());
        }

        /// <summary>
        ///A test for Pop
        ///</summary>
        //[TestMethod()]
        //public void PopTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call PopTestHelper<T>() with appropriate type parameters.");
        //}

        /// <summary>
        ///A test for Push
        ///</summary>
        [TestMethod()]
        public void PushTest()
        {
            ItemQueue q = new ItemQueue();
   
            // push the capacity.
            for (int i = 0; i < 1000; i++)
                q.Push(new Item(i));

            Assert.AreEqual(1000, q.Count);

            for (int v = 0; v < 1000; v++)
                Assert.AreEqual(v, q.Pop().Value);

            Assert.AreEqual(0, q.Count);


            // push the capacity.
            for (int i = 0; i < 1000; i++)
                q.Push(new Item(999-i));

            Assert.AreEqual(1000, q.Count);

            for (int v = 0; v < 1000; v++)
                Assert.AreEqual(v, q.Pop().Value);

            Assert.AreEqual(0, q.Count);

            ItemQueue r = CreateReversedQueue(new int[] { });

            // push the capacity.
            for (int i = 0; i < 1000; i++)
                r.Push(new Item(i));

            Assert.AreEqual(1000, r.Count);

            for (int v = 999; v >= 0; v--)
                Assert.AreEqual(v, r.Pop().Value);

            Assert.AreEqual(0, r.Count);


        }

        ///// <summary>
        /////A test for Swap
        /////</summary>
        //public void SwapTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    int i = 0; // TODO: Initialize to an appropriate value
        //    int j = 0; // TODO: Initialize to an appropriate value
        //    target.Swap(i, j);
        //    Assert.Inconclusive("A method that does not return a value cannot be verified.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void SwapTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call SwapTestHelper<T>() with appropriate type parameters.");
        //}

        ///// <summary>
        /////A test for Up
        /////</summary>
        //public void UpTestHelper<T>()
        //    where T : IComparable<T>
        //{
        //    PriorityQueue_Accessor<T> target = new PriorityQueue_Accessor<T>(); // TODO: Initialize to an appropriate value
        //    int j = 0; // TODO: Initialize to an appropriate value
        //    target.Up(j);
        //    Assert.Inconclusive("A method that does not return a value cannot be verified.");
        //}

        //[TestMethod()]
        //[DeploymentItem("TaskExtensions.dll")]
        //public void UpTest()
        //{
        //    Assert.Inconclusive("No appropriate type parameter is found to satisfies the type constraint(s) of T. " +
        //            "Please call UpTestHelper<T>() with appropriate type parameters.");
        //}

        /// <summary>
        ///A test for Count
        ///</summary>
        [TestMethod()]
        public void CountTest()
        {
            ItemQueue q = CreateQueue(new int[] { 4, 2, 3, 1 });
            Assert.AreEqual(4, q.Count);

            q.Pop();
            Assert.AreEqual(3, q.Count);

            q.Push(new Item(12));
            Assert.AreEqual(4, q.Count);

            ItemQueue e = new ItemQueue();
            Assert.AreEqual(0, e.Count);

            e.Pop();
            Assert.AreEqual(0, e.Count);
        }
    }
}

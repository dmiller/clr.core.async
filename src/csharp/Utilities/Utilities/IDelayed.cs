using System;

namespace clojure.core.async
{
    public interface IDelayed : IComparable<IDelayed>
    {
        TimeSpan GetRemainingDelay();
    }
}

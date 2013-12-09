using System;

namespace Clr.Util.Concurrent
{
    public interface IDelayed : IComparable<IDelayed>
    {
        TimeSpan GetRemainingDelay();
    }
}

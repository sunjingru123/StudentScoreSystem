"""快速排序的几种实现，用于演示与基准测试。"""


def quicksort(nums: list[int]) -> list[int]:
    """返回一个升序排列的新列表（简单递归版）。"""
    if len(nums) <= 1:
        return nums
    pivot = nums[len(nums) // 2]
    less = [x for x in nums if x < pivot]
    equal = [x for x in nums if x == pivot]
    greater = [x for x in nums if x > pivot]
    return quicksort(less) + equal + quicksort(greater)


def quicksort_inplace(nums: list[int], low: int = 0, high: int = None) -> None:
    """原地排序（Lomuto 分区），直接修改传入的列表。"""
    if high is None:
        high = len(nums) - 1
    if low >= high:
        return
    pivot = nums[high]
    i = low
    for j in range(low, high):
        if nums[j] <= pivot:
            nums[i], nums[j] = nums[j], nums[i]
            i += 1
    nums[i], nums[high] = nums[high], nums[i]
    quicksort_inplace(nums, low, i - 1)
    quicksort_inplace(nums, i + 1, high)


if __name__ == "__main__":
    import random

    data = [random.randint(0, 100) for _ in range(10)]
    print("原始:", data)
    print("递归版:", quicksort(data))

    arr = data[:]
    quicksort_inplace(arr)
    print("原地版:", arr)
    assert arr == sorted(data)
    print("验证通过 ✓")

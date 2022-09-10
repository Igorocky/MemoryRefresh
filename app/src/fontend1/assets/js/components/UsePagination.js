"use strict";

function usePagination({items, pageSize, onlyArrowButtons = false}) {
    let [currPageIdx, setCurrPageIdx] = useState(0)

    const itemsAmount = items?.length ?? 0
    const numberOfPages = Math.floor(itemsAmount / pageSize) + (itemsAmount % pageSize > 0 ? 1 : 0)
    if (currPageIdx >= numberOfPages) {
        currPageIdx = numberOfPages-1
        setCurrPageIdx(currPageIdx)
    }
    const pageFirstItemIdx = currPageIdx*pageSize
    const pageLastItemIdx = pageFirstItemIdx+pageSize-1

    function renderPaginationControls({onPageChange}) {
        return re(Pagination, {
            numOfPages:numberOfPages,
            curIdx:currPageIdx,
            onChange: newPageIdx => {
                setCurrPageIdx(newPageIdx)
                onPageChange?.(newPageIdx)
            },
            onlyArrowButtons
        })
    }

    return {
        pageFirstItemIdx,
        pageLastItemIdx,
        renderPaginationControls,
        setCurrPageIdx
    }
}
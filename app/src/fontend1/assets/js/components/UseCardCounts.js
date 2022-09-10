"use strict";

function useCardCounts() {
    const [counts, setCounts] = useState({})
    const [numberOfFullCycles, setNumberOfFullCycles] = useState(0)

    function getNumberOfCompletedCardsInCycle(cards) {
        if (hasNoValue(cards)) {
            return 0
        } else {
            return cards.map(c=>c.id).filter(id => counts[id]).length
        }
    }

    function getNextCardId(cards) {
        const candidates = cards.map(c=>c.id).filter(id => !counts[id])
        if (candidates.length) {
            return candidates[randomInt(0,candidates.length-1)]
        } else {
            setCounts({})
            setNumberOfFullCycles(prev => prev + 1)
            return cards[randomInt(0,cards.length-1)].id
        }
    }

    function countCard(id) {
        counts[id] = true
        setCounts(prev => ({...prev, [id]:true}))
    }

    function resetCardCounts() {
        setCounts({})
        setNumberOfFullCycles(0)
    }

    return {getNextCardId, countCard, numberOfFullCycles, getNumberOfCompletedCardsInCycle, resetCardCounts}
}
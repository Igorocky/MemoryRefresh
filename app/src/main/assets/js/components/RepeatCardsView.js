"use strict";

const RepeatCardsView = ({query,openView,setPageTitle,controlsContainer, cycledMode}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError, showMessageWithProgress} = useMessagePopup()

    const af = AVAILABLE_TRANSLATE_CARD_FILTERS

    const [isFilterMode, setIsFilterMode] = useState(true)

    const {allTags, allTagsMap, errorLoadingTags} = useTags()

    const {getNextCardId, countCard, numberOfFullCycles, getNumberOfCompletedCardsInCycle, resetCardCounts} = useCardCounts()

    const [cardCounter, setCardCounter] = useState(0)
    const [cardUpdateCounter, setCardUpdateCounter] = useState(0)
    const [filter, setFilter] = useState({})
    const filterInitialState = useMemo(() => {
        if (query.filterFromLocalStor) {
            return readFromLocalStorage(CARDS_FILTER_LOCAL_STOR_KEY, {})
        } else if (!cycledMode) {
            return {}
        } else {
            return null
        }
    }, [])
    const [allCards, setAllCards] = useState(null)
    const [nextCardWillBeAvailableIn, setNextCardWillBeAvailableIn] = useState(null)
    const [errorLoadingCards, setErrorLoadingCards] = useState(null)

    const [cardToRepeat, setCardToRepeatOrig] = useState(null)
    function setCardToRepeat(card) {
        setCardToRepeatOrig(card)
        setCardCounter(prev => prev+1)
    }

    async function reloadCards({filter}) {
        setAllCards(null)
        setCardToRepeat(null)
        setErrorLoadingCards(null)
        setNextCardWillBeAvailableIn(null)
        if (!cycledMode) {
            const res = await be.selectTopOverdueTranslateCards(filter)
            if (res.err) {
                setErrorLoadingCards(res.err)
                showError(res.err)
            } else {
                if (res.data?.cards?.length??0) {
                    const allCards = res.data.cards
                    setAllCards(allCards)
                    proceedToNextCard({cards:allCards})
                } else {
                    setAllCards([])
                    setNextCardWillBeAvailableIn(res.data?.nextCardIn)
                }
            }
        } else {
            const res = await be.readTranslateCardsByFilter(filter)
            if (res.err) {
                setErrorLoadingCards(res.err)
                showError(res.err)
            } else {
                const allCards = res.data.cards
                setAllCards(allCards)
                proceedToNextCard({cards:allCards})
            }
        }
    }

    function proceedToNextCard({cards}) {
        if (!cycledMode) {
            if (hasValue(cards)) {
                if (cards.length) {
                    setCardToRepeat(cards[0])
                }
            } else {
                const newAllCards = allCards.filter(c=>c.id !== cardToRepeat.id)
                if (newAllCards.length) {
                    setAllCards(newAllCards)
                    setCardToRepeat(newAllCards[0])
                } else {
                    reloadCards({filter})
                }
            }
        } else {
            cards = cards??allCards
            if (cards.length) {
                const nextCardId = getNextCardId(cards??allCards)
                setCardToRepeat(cards.find(c=>c.id===nextCardId))
            }
        }
    }

    function renderCardToRepeat() {
        if (!isFilterMode) {
            if (errorLoadingCards) {
                return `An error occurred during loading of cards: [${errorLoadingCards.code}] - ${errorLoadingCards.msg}`
            } else if (hasValue(nextCardWillBeAvailableIn) && nextCardWillBeAvailableIn !== '') {
                return RE.Container.col.top.left({},{},
                    `Next card will be available in ${nextCardWillBeAvailableIn}`,
                    renderRefreshButton()
                )
            } else if (hasNoValue(allCards)) {
                return `Loading cards...`
            } else if (hasNoValue(cardToRepeat)) {
                return `No cards matching the search criteria were found.`
            } else {
                return re(RepeatTranslateCardCmp, {
                    key: cardCounter,
                    allTags,
                    allTagsMap,
                    cardToRepeat,
                    onCardWasUpdated: () => {
                        setCardUpdateCounter(prev => prev + 1)
                    },
                    onCardWasDeleted: () => {
                        setCardUpdateCounter(prev => prev + 1)
                        reloadCards({filter})
                    },
                    onDone: ({cardWasUpdated}) => {
                        if (cycledMode) {
                            countCard(cardToRepeat.id)
                        }
                        cardWasUpdated ? reloadCards({filter}) : proceedToNextCard({})
                    },
                    controlsContainer,
                    cycledMode
                })
            }
        }
    }

    function renderRefreshButton() {
        return iconButton({iconName:'refresh', onClick: () => reloadCards({filter})})
    }

    function openFilter() {
        setIsFilterMode(true)
        setCardToRepeat(null)
        setAllCards(null)
        setErrorLoadingCards(null)
        setNextCardWillBeAvailableIn(null)
        resetCardCounts()
    }

    function renderFilter() {
        return re(TranslateCardFilterCmp, {
            allTags,
            allTagsMap,
            initialState: filterInitialState,
            submitButtonIconName:'play_arrow',
            onSubmit: filter => {
                setIsFilterMode(false)
                setFilter(filter)
                reloadCards({filter})
            },
            allowedFilters:cycledMode?null:[af.INCLUDE_TAGS, af.EXCLUDE_TAGS, af.FOREIGN_TEXT_LENGTH],
            minimized: !isFilterMode,
            cardUpdateCounter
        })
    }

    function renderOpenSearchButton() {
        return iconButton({iconName:'youtube_searched_for', onClick: openFilter})
    }

    function renderPageContent() {
        if (errorLoadingTags) {
            return RE.Container.col.top.left({},{},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
                renderRefreshButton()
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style: {marginTop:'5px'}},{style: {marginBottom:'5px'}},
                renderFilter(),
                renderCardToRepeat()
            )
        }
    }

    return RE.Fragment({},
        RE.If(controlsContainer?.current && hasValue(allCards), () => RE.Portal({container:controlsContainer.current},
            RE.span({style:{marginLeft:'15px'}},
                cycledMode
                    ?`${allCards?.length??0}: ${numberOfFullCycles}/${getNumberOfCompletedCardsInCycle(allCards)}`
                    :`${allCards?.length??0}`
            ),
            renderOpenSearchButton()
        )),
        renderPageContent(),
        renderMessagePopup()
    )
}

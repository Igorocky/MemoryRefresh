"use strict";

const CARDS_FILTER_LOCAL_STOR_KEY = 'cards-filter'

const CardsSearchView = ({query,openView,setPageTitle,controlsContainer}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError, showMessageWithProgress} = useMessagePopup()

    const [isFilterMode, setIsFilterMode] = useState(true)
    const filterStateRef = useRef(null)

    const {allTags, allTagsMap, errorLoadingTags} = useTags()

    const [allCards, setAllCards] = useState(null)
    const [errorLoadingCards, setErrorLoadingCards] = useState(null)

    const pageSize = 100
    const {setCurrPageIdx,renderPaginationControls,pageFirstItemIdx,pageLastItemIdx} =
        usePagination({items:allCards, pageSize: pageSize, onlyArrowButtons:true})

    const [focusedCardId, setFocusedCardId] = useState(null)
    const [cardToEdit, setCardToEdit] = useState(null)

    const [cardUpdateCounter, setCardUpdateCounter] = useState(0)

    useEffect(() => {
        if (hasValue(focusedCardId) && hasNoValue(cardToEdit)) {
            document.getElementById(focusedCardId)?.scrollIntoView()
        }
    }, [cardToEdit])

    async function reloadCards({filter}) {
        setAllCards(null)
        const res = await be.readTranslateCardsByFilter(filter)
        if (res.err) {
            setErrorLoadingCards(res.err)
            showError(res.err)
        } else {
            const allCards = res.data.cards
            for (const card of allCards) {
                card.tagIds = card.tagIds.map(id => allTagsMap[id]).sortBy('name').map(t=>t.id)
            }
            setAllCards(allCards)
            setCurrPageIdx(0)
        }
    }

    function renderListOfCards() {
        if (!isFilterMode) {
            if (hasNoValue(allCards)) {
                return 'Loading cards...'
            } else if (allCards.length == 0) {
                return 'There are no cards matching the search criteria.'
            } else {
                return RE.Container.col.top.left({},{style:{marginTop: '10px'}},
                    RE.If(allCards.length > pageSize, () => renderPaginationControls({})),
                    re(ListOfObjectsCmp,{
                        objects: allCards,
                        beginIdx: pageFirstItemIdx,
                        endIdx: pageLastItemIdx,
                        onObjectClicked: cardId => setFocusedCardId(prev => prev !== cardId ? cardId : null),
                        renderObject: (card,idx) => RE.Paper({id:card.id,style:{backgroundColor:card.paused?'rgb(242, 242, 242)':'rgb(255, 249, 230)'}}, renderCard(card,idx))
                    }),
                    RE.If(allCards.length > pageSize, () => renderPaginationControls({onPageChange: () => window.scrollTo(0, 0)})),
                )
            }
        }
    }

    async function deleteCard({card}) {
        if (await confirmAction({text: `Delete this card? "${truncateToMaxLength(20,card.textToTranslate)}"`, okBtnColor: 'secondary'})) {
            setCardUpdateCounter(prev => prev + 1)
            const closeProgressIndicator = showMessageWithProgress({text: 'Deleting...'})
            const res = await be.deleteTranslateCard({cardId:card.id})
            closeProgressIndicator()
            if (res.err) {
                await showError(res.err)
                openFilter()
            } else {
                setAllCards(prev => prev.filter(c => c.id !== card.id))
            }
        }
    }

    function truncateToMaxLength(maxLength,text) {
        return text.substring(0,maxLength) + (text.length > maxLength ? '...' : '')
    }

    function renderCardText(text) {
        if (text.indexOf('\n') >= 0) {
            return multilineTextToTable({text})
        } else {
            return text
        }
    }

    function renderCard(card,idx) {
        const cardElem = RE.Fragment({},
            RE.div(
                {style:{borderBottom:'solid 1px lightgrey', padding:'3px'}},
                RE.span({style:{fontWeight:'bold'}},`${idx+1}. `),
                renderListOfTags({
                    tags: card.tagIds.map(id => allTagsMap[id]),
                    color:card.paused?'grey':'black',
                })
            ),
            RE.div(
                {style:{borderBottom:'solid 1px lightgrey', padding:'3px'}},
                RE.span({style:{color:card.paused?'grey':'black'}},renderCardText(card.textToTranslate))
            ),
            RE.div(
                {},
                RE.span({style:{color:card.paused?'grey':'black', padding:'3px'}},renderCardText(card.translation))
            ),
        )
        if (focusedCardId === card.id) {
            return RE.Container.col.top.left({}, {},
                RE.Container.row.left.center({}, {},
                    iconButton({iconName: 'delete', onClick: () => deleteCard({card})}),
                    iconButton({iconName: 'edit', onClick: () => setCardToEdit(card)}),
                ),
                cardElem
            )
        } else {
            return cardElem
        }
    }

    function openFilter() {
        setCardToEdit(null)
        setIsFilterMode(true)
        setAllCards(null)
        setFocusedCardId(null)
    }

    function renderFilter() {
        return re(TranslateCardFilterCmp, {
            allTags,
            allTagsMap,
            stateRef: filterStateRef,
            onSubmit: filter => {
                setIsFilterMode(false)
                reloadCards({filter})
            },
            minimized: !isFilterMode,
            cardUpdateCounter
        })
    }

    function renderFastRepeatButton() {
        return iconButton({iconName:'speed', onClick: () => {
            saveToLocalStorage(CARDS_FILTER_LOCAL_STOR_KEY, filterStateRef.current)
            openView(FAST_REPEAT_CARDS_VIEW, {filterFromLocalStor: true})
        }})
    }

    function renderPageContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap)) {
            return 'Loading tags...'
        } else {
            let cardsOrEditCmp
            if (hasValue(cardToEdit)) {
                cardsOrEditCmp = re(EditTranslateCardCmp,{
                    allTags, allTagsMap, card:cardToEdit,
                    onCancelled: () => setCardToEdit(null),
                    onSaved: async () => {
                        setCardUpdateCounter(prev => prev + 1)
                        const closeProgressIndicator = showMessageWithProgress({text: 'Reloading changed card...'})
                        const res = await be.readTranslateCardById({cardId: cardToEdit.id})
                        closeProgressIndicator()
                        if (res.err) {
                            await showError(res.err)
                            openFilter()
                        } else {
                            setCardToEdit(null)
                            setAllCards(prev => prev.map(card => card.id === cardToEdit.id ? res.data : card))
                        }
                    },
                    onDeleted: () => {
                        setCardUpdateCounter(prev => prev + 1)
                        setCardToEdit(null)
                        setAllCards(prev => prev.filter(card => card.id !== cardToEdit.id))
                    }
                })
            } else {
                cardsOrEditCmp = renderListOfCards()
            }
            return RE.Container.col.top.left({style: {marginTop:'5px'}},{style:{marginBottom:'10px'}},
                renderFilter(),
                cardsOrEditCmp
            )
        }
    }

    return RE.Fragment({},
        RE.If(controlsContainer?.current, () => RE.Portal({container:controlsContainer.current},
            RE.If(!isFilterMode && hasValue(allCards), () => RE.span({style:{marginLeft:'10px'}}, allCards.length)),
            RE.IfNot(isFilterMode, () => RE.Fragment({},
                iconButton({iconName:'youtube_searched_for', onClick: openFilter})
            )),
            RE.If(!isFilterMode && hasNoValue(cardToEdit) && allCards?.length, () => renderFastRepeatButton())
        )),
        renderPageContent(),
        renderMessagePopup()
    )
}

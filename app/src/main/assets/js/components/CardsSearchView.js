"use strict";

const CardsSearchView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError} = useMessagePopup()

    const [isFilterMode, setIsFilterMode] = useState(true)

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [allCards, setAllCards] = useState(null)
    const [errorLoadingCards, setErrorLoadingCards] = useState(null)

    const [focusedCardId, setFocusedCardId] = useState(null)

    useEffect(async () => {
        const res = await be.readAllTags()
        if (res.err) {
            setErrorLoadingTags(res.err)
            showError(res.err)
        } else {
            const allTags = res.data
            setAllTags(allTags)
            const allTagsMap = {}
            for (const tag of allTags) {
                allTagsMap[tag.id] = tag
            }
            setAllTagsMap(allTagsMap)
        }
    }, [])

    async function reloadCards({filter}) {
        setAllCards(null)
        const res = await be.readTranslateCardsByFilter(filter)
        if (res.err) {
            setErrorLoadingCards(res.err)
            showError(res.err)
        } else {
            const allCards = res.data
            for (const card of allCards) {
                card.tagIds = card.tagIds.map(id => allTagsMap[id]).sortBy('name').map(t=>t.id)
            }
            setAllCards(allCards)
        }
    }

    function renderListOfCards() {
        if (!isFilterMode) {
            if (hasNoValue(allCards)) {
                return 'Loading cards...'
            } else if (allCards.length == 0) {
                return 'There are no cards matching the search criteria.'
            } else {
                return RE.Container.col.top.left({},{},
                    RE.span({style: {fontWeight:'bold'}}, `Cards found (${allCards.length}):`),
                    re(ListOfObjectsCmp,{
                        objects: allCards,
                        beginIdx: 0,
                        endIdx: allCards.length-1,
                        onObjectClicked: cardId => setFocusedCardId(prev => prev !== cardId ? cardId : null),
                        renderObject: renderCard
                    })
                )
            }
        }
    }

    // async function deleteTag({tag}) {
    //     if (await confirmAction({text: `Delete tag '${tag.name}'?`, okBtnColor: 'secondary'})) {
    //         const res = await be.deleteTag({tagId:tag.id})
    //         if (res.err) {
    //             showError(res.err)
    //         } else {
    //             reloadAllTags()
    //         }
    //     }
    // }

    function truncateToMaxLength(maxLength,text) {
        return text.substring(0,maxLength) + (text.length > maxLength ? '...' : '')
    }

    function renderCard(card,idx) {
        const maxTextLength = 30
        const cardElem = RE.Fragment({},
            RE.div(
                {style:{borderBottom:'solid 1px lightgrey', padding:'3px'}},
                RE.span({style:{fontWeight:'bold'}},`${idx+1}. `),
                card.tagIds.map(tagId => RE.span(
                    {
                        key:tagId,
                        style:{
                            color:card.paused?'grey':'black',
                            display: 'inline-block',
                            border:'solid 1px grey',
                            borderRadius:'10px',
                            paddingLeft:'4px',
                            paddingRight:'4px',
                            marginRight:'1px'
                        }
                    },
                    allTagsMap[tagId].name
                ))
            ),
            RE.div(
                {style:{borderBottom:'solid 1px lightgrey', padding:'3px'}},
                RE.span({style:{color:card.paused?'grey':'black'}},truncateToMaxLength(maxTextLength,card.textToTranslate))
            ),
            RE.div(
                {},
                RE.span({style:{color:card.paused?'grey':'black', padding:'3px'}},truncateToMaxLength(maxTextLength,card.translation))
            ),
        )
        if (focusedCardId === card.id) {
            return RE.Container.col.top.left({}, {},
                RE.Container.row.left.center({}, {},
                    iconButton({iconName: 'delete', onClick: () => null}),
                    iconButton({iconName: 'edit', onClick: () => null}),
                ),
                cardElem
            )
        } else {
            return cardElem
        }
    }

    function renderFilter() {
        return RE.Container.col.top.left({},{},
            RE.IfNot(isFilterMode, () => RE.Fragment({},
                iconButton({
                    iconName:'youtube_searched_for',
                    onClick: () => {
                        setIsFilterMode(true)
                        setAllCards(null)
                    }
                }),
                RE.span({style: {fontWeight:'bold'}}, 'Filters:')
            )),
            re(TranslateCardFilterCmp, {
                allTags,
                allTagsMap,
                onSubmit: filter => {
                    setIsFilterMode(false)
                    reloadCards({filter})
                },
                minimized: !isFilterMode
            })
        )
    }

    function renderPageContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style: {marginTop:'5px'}},{style:{marginBottom:'10px'}},
                renderFilter(),
                renderListOfCards()
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}

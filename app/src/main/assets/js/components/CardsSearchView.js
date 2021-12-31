"use strict";

const CardsSearchView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError} = useMessagePopup()

    const [isFilterMode, setIsFilterMode] = useState(true)

    const [allCards, setAllCards] = useState(null)
    const [errorLoadingCards, setErrorLoadingCards] = useState(null)

    const [focusedCardId, setFocusedCardId] = useState(null)

    async function reloadCards({filter}) {
        const res = await be.readTranslateCardsByFilter(filter)
        if (res.err) {
            setErrorLoadingCards(res.err)
            showError(res.err)
        } else {
            setAllCards(res.data)
        }
    }

    function renderListOfCards() {
        if (!isFilterMode) {
            if (allCards.length == 0) {
                return 'There are no cards matching the search criteria.'
            } else {
                return re(ListOfObjectsCmp,{
                    objects: allCards,
                    beginIdx: 0,
                    endIdx: allCards.length-1,
                    onObjectClicked: cardId => setFocusedCardId(prev => prev !== cardId ? cardId : null),
                    renderObject: renderCard
                })
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

    function renderCard(card) {
        return 'card'
        // if (focusedCardId === card.id) {
        //     if (editMode) {
        //         return re(EditTagCmp, {
        //             tagId: card.id,
        //             tagName: card.name,
        //             onSaved: () => {
        //                 setEditMode(false)
        //                 reloadAllTags()
        //             },
        //             onCanceled: () => setEditMode(false)
        //         })
        //     } else {
        //         return RE.Container.row.left.center({},{},
        //             iconButton({iconName: 'delete', onClick: () => deleteTag({tag: card})}),
        //             iconButton({iconName: 'edit', onClick: () => setEditMode(true)}),
        //             RE.span({style: {marginLeft:'5px', marginRight:'5px'}}, card.name)
        //         )
        //     }
        // } else {
        //     return RE.span({style: {marginLeft:'5px', marginRight:'5px'}}, card.name)
        // }
    }

    function renderFilter() {
        return re(TranslateCardFilterCmp, {

        })
    }

    function renderPageContent() {
        return RE.Container.col.top.left({style: {marginTop:'5px'}},{},
            renderFilter(),
            // renderListOfCards()
        )
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}

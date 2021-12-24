"use strict";

const RepeatCardsView = ({query,openView,setPageTitle,controlsContainer}) => {
    const {showError, renderMessagePopup} = useMessagePopup()

    const [cardCounter, setCardCounter] = useState(0)
    const [nextCardResponse, setNextCardResponse] = useState(null)
    const [errorLoadingCard, setErrorLoadingCard] = useState(null)

    useEffect(() => {
        loadNextCard()
    }, [])

    async function loadNextCard() {
        setNextCardResponse(null)
        setErrorLoadingCard(null)
        const res = await be.getNextCardToRepeat()
        if (res.err) {
            await showError(res.err)
            setErrorLoadingCard(res.err)
            setNextCardResponse(null)
        } else {
            setErrorLoadingCard(null)
            setNextCardResponse(res.data)
            setCardCounter(cnt => cnt+1)
        }
    }

    function renderRefreshButton() {
        return iconButton({iconName:'refresh', onClick: loadNextCard})
    }

    function renderPageContent() {
        if (errorLoadingCard) {
            return RE.Fragment({},
                `An error occurred during card loading: [${errorLoadingCard.code}] - ${errorLoadingCard.msg}`,
                renderRefreshButton()
            )
        } else if (!nextCardResponse) {
            return 'Loading card...'
        } else {
            if (nextCardResponse.cardsRemain > 0) {
                return re(RepeatTranslateCardCmp, {
                    key: cardCounter,
                    cardId:nextCardResponse.cardId,
                    cardsRemain:nextCardResponse.cardsRemain,
                    onDone: loadNextCard,
                    controlsContainer
                })
            } else if (nextCardResponse.nextCardIn === '') {
                return `There are no cards to repeat.`
            } else {
                return RE.Fragment({},
                    `Next card will be available in ${nextCardResponse.nextCardIn}`,
                    renderRefreshButton()
                )
            }
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}

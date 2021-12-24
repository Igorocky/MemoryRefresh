"use strict";

const RepeatTranslateCardCmp = ({cardCounter,cardId,cardsRemain,onDone}) => {
    const USER_INPUT_TEXT_FIELD = 'user-input'
    const CARD_DELAY_TEXT_FIELD = 'card-delay'

    const {showError, renderMessagePopup} = useMessagePopup()

    const [errorLoadingCard, setErrorLoadingCard] = useState(null)
    const [card, setCard] = useState(null)
    const [userInput, setUserInput] = useState('')
    const [answerFromBE, setAnswerFromBE] = useState(null)
    const [beValidationResult, setBeValidationResult] = useState(null)
    const [delay, setDelay] = useState(null)
    const [autoFocusDelay, setAutoFocusDelay] = useState(false)
    const delayTextField = useRef(null)
    const [validateButtonWasClicked, setValidateButtonWasClicked] = useState(false)

    useEffect(async () => {
        resetAll()
        const resp = await be.getTranslateCardById({cardId})
        if (!resp.err) {
            setCard(resp.data)
            setDelay(resp.data.schedule.delay)
        } else {
            await showError(resp.err)
            setErrorLoadingCard(resp.err)
        }
    }, [cardCounter])

    useEffect(() => {
        if (autoFocusDelay && delayTextField.current) {
            const delayInput = document.getElementById(CARD_DELAY_TEXT_FIELD)
            delayInput?.focus()
            delayInput?.select()
            delayInput?.scrollIntoView()
        }
    }, [delayTextField.current])

    function resetAll() {
        setErrorLoadingCard(null)
        setCard(null)
        setUserInput('')
        setAnswerFromBE(null)
        setBeValidationResult(null)
        setDelay(null)
        setAutoFocusDelay(false)
        setValidateButtonWasClicked(false)
    }

    function renderQuestion() {
        if (card) {
            return RE.Container.col.top.left({},{},
                RE.div({style:{fontWeight:'bold',marginBottom:'10px'}}, 'Translate:'),
                RE.div({}, card.textToTranslate),
            )
        }
    }

    function renderExpectedTranslation() {
        if (answerFromBE) {
            return RE.Container.col.top.left({},{},
                RE.div({}, 'Expected:'),
                RE.div({}, answerFromBE),
            )
        }
    }

    function shouldHighlightUserInputInRed() {
        return beValidationResult === false && isUserInputCorrect() === false
    }

    function getUserInputBackgroundColor() {
        if (hasValue(answerFromBE) && isUserInputCorrect()) {
            return '#c6ebc6'
        } else {
            return shouldHighlightUserInputInRed() ? '#ffb3b3' : ''
        }
    }

    function renderUserTranslation() {
        return RE.TextField({
            id: USER_INPUT_TEXT_FIELD,
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            autoFocus: true,
            value: userInput,
            label: 'Translation',
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            style: {backgroundColor:getUserInputBackgroundColor()},
            onChange: event => {
                onUserInputChange({newUserInput:event.nativeEvent.target.value})
            },
            onKeyUp: event => (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) ? validateTranslation() : null,
        })
    }

    async function validateTranslation() {
        if (hasNoValue(beValidationResult)) {
            const res = await be.validateTranslateCard({cardId, userProvidedTranslation: userInput})
            if (res.err) {
                await showError(res.err)
                return false
            } else {
                setBeValidationResult(res.data.isCorrect)
                setAnswerFromBE(res.data.answer)
                if (res.data.isCorrect) {
                    setAutoFocusDelay(true)
                }
                return res.data.isCorrect
            }
        }
    }

    function isUserInputCorrect() {
        return hasNoValue(answerFromBE) ? undefined : answerFromBE == userInput.trim()
    }

    function onUserInputChange({newUserInput}) {
        if (newUserInput != userInput) {
            setUserInput(newUserInput)
        }
    }

    function renderDelay() {
        return RE.TextField({
            ref: delayTextField,
            id: CARD_DELAY_TEXT_FIELD,
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            // autoFocus: autoFocusDelay,
            value: delay,
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != delay) {
                    setDelay(newText)
                }
            },
            onKeyUp: event => (event.keyCode === ENTER_KEY_CODE) ? updateSchedule() : null,
        })
    }

    function updateSchedule() {
        const res = be.updateTranslateCard({cardId, delay, recalculateDelay: true})
        if (res.err) {
            showError(res.err)
        } else {
            resetAll()
            onDone()
        }
    }

    function renderCardsRemaining() {
        return RE.div({}, `Cards remaining: ${cardsRemain}`)
    }

    function renderValidateButton() {
        const disabled = hasValue(answerFromBE)
        return iconButton({
            iconName:'done',
            onClick: async () => {
                setValidateButtonWasClicked(true)
                if (!(await validateTranslation())) {
                    const userTextInput = document.getElementById(USER_INPUT_TEXT_FIELD)
                    userTextInput?.focus()
                    userTextInput?.scrollIntoView()
                }
            },
            disabled,
            iconStyle:{color:disabled?'lightgrey':'blue'}
        })
    }

    function renderNextButton() {
        return RE.Button({
            variant: 'contained',
            color: 'primary',
            onClick: updateSchedule,
        }, 'Next')
    }

    function renderValidateButtonOrExpectedTranslation() {
        if (hasNoValue(answerFromBE) || beValidationResult === true || !validateButtonWasClicked) {
            return renderValidateButton()
        } else {
            return renderExpectedTranslation()
        }
    }

    function renderPageContent() {
        if (errorLoadingCard) {
            return `An error occurred during card loading: [${errorLoadingCard.code}] - ${errorLoadingCard.msg}`
        } else if (!card) {
            return 'Loading card...'
        } else {
            const shouldRenderExpectedAnswer = hasValue(answerFromBE) && !(beValidationResult || isUserInputCorrect())
            const shouldRenderDelay = hasValue(answerFromBE) && isUserInputCorrect();
            return RE.Container.col.top.left({},{style: {marginTop: '10px'}},
                renderCardsRemaining(),
                renderQuestion(),
                renderValidateButtonOrExpectedTranslation(),
                renderUserTranslation(),
                RE.If(!validateButtonWasClicked && shouldRenderExpectedAnswer, renderExpectedTranslation),
                RE.If(shouldRenderDelay, renderDelay),
                RE.If(shouldRenderDelay, renderNextButton),
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )

}

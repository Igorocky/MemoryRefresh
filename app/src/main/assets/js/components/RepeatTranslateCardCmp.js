"use strict";

const RepeatTranslateCardCmp = ({cardId,cardsRemain,onDone}) => {
    const USER_INPUT_TEXT_FIELD = 'user-input'
    const CARD_DELAY_TEXT_FIELD = 'card-delay'

    const {showError, showMessage, renderMessagePopup} = useMessagePopup()

    const [errorLoadingCard, setErrorLoadingCard] = useState(null)
    const [card, setCard] = useState(null)
    const [userInput, setUserInput] = useState('')
    const [answerFromBE, setAnswerFromBE] = useState(null)
    const [beValidationResult, setBeValidationResult] = useState(null)
    const [delay, setDelay] = useState(null)
    const [autoFocusDelay, setAutoFocusDelay] = useState(false)
    const delayTextField = useRef(null)
    const [validateButtonWasClicked, setValidateButtonWasClicked] = useState(false)

    const [editMode, setEditMode] = useState(false)

    useEffect(async () => {
        loadCard()
    }, [])

    useEffect(() => {
        if (autoFocusDelay && delayTextField.current) {
            const delayInput = document.getElementById(CARD_DELAY_TEXT_FIELD)
            delayInput?.focus()
            delayInput?.select()
            delayInput?.scrollIntoView()
            setAutoFocusDelay(false)
        }
    }, [delayTextField.current])

    async function loadCard() {
        setCard(null)
        const resp = await be.getTranslateCardById({cardId})
        if (resp.err) {
            await showError(resp.err)
            setErrorLoadingCard(resp.err)
        } else {
            setCard(resp.data)
            setDelay(resp.data.schedule.delay)
            if (editMode && hasValue(answerFromBE)) {
                console.log('resp.data.translation.trim()', resp.data.translation.trim())
                setAnswerFromBE(resp.data.translation.trim())
            }
        }
        setEditMode(false)
    }

    function renderQuestion() {
        if (card) {
            return RE.Container.col.top.left({},{},
                RE.div({style:{fontWeight:'bold',marginBottom:'10px'}},
                    renderEditButton(),
                    'Translate:'
                ),
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
            if (userInput.trim().length === 0) {
                showMessage({text: 'Translation must not be empty.'})
                setValidateButtonWasClicked(false)
            } else {
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
            value: delay??'',
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

    function renderEditButton() {
        return iconButton({iconName:'edit', onClick: () => setEditMode(true)})
    }

    function renderPageContent() {
        if (errorLoadingCard) {
            return `An error occurred during card loading: [${errorLoadingCard.code}] - ${errorLoadingCard.msg}`
        } else if (hasNoValue(card)) {
            return 'Loading card...'
        } else if (editMode) {
            return re(EditTranslateCardCmp, {
                card,
                translationEnabled: hasValue(answerFromBE),
                onDone: loadCard
            })
        } else {
            const shouldRenderExpectedTranslationAbove = hasValue(answerFromBE) && validateButtonWasClicked
            const shouldRenderExpectedTranslationBelow = hasValue(answerFromBE) && !validateButtonWasClicked && !isUserInputCorrect()
            const shouldRenderDelay = hasValue(answerFromBE) && isUserInputCorrect();
            return RE.Container.col.top.left({},{style: {marginTop: '10px'}},
                renderCardsRemaining(),
                renderQuestion(),
                RE.If(shouldRenderExpectedTranslationAbove, renderExpectedTranslation),
                RE.If(!shouldRenderExpectedTranslationAbove, renderValidateButton),
                renderUserTranslation(),
                RE.If(shouldRenderExpectedTranslationBelow, renderExpectedTranslation),
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

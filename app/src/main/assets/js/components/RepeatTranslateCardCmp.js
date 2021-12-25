"use strict";

const RepeatTranslateCardCmp = ({cardId,cardsRemain,onDone,controlsContainer}) => {
    const USER_INPUT_TEXT_FIELD = 'user-input'
    const CARD_DELAY_TEXT_FIELD = 'card-delay'

    const {renderMessagePopup, showError, showMessage} = useMessagePopup()

    const [errorLoadingCard, setErrorLoadingCard] = useState(null)
    const [card, setCard] = useState(null)
    const [userInput, setUserInput] = useState('')
    const [validationRequestIsInProgress, setValidationRequestIsInProgress] = useState(false)
    const [answerFromBE, setAnswerFromBE] = useState(null)
    const [answerFromBEIsShown, setAnswerFromBEIsShown] = useState(false)
    const [beValidationResult, setBeValidationResult] = useState(null)
    const [delay, setDelay] = useState(null)
    const [autoFocusDelay, setAutoFocusDelay] = useState(false)
    const delayTextField = useRef(null)
    const [updateDelayRequestIsInProgress, setUpdateDelayRequestIsInProgress] = useState(false)

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
                setAnswerFromBE(resp.data.translation.trim())
            }
        }
        setEditMode(false)
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
                RE.div({style:{fontWeight:'bold',marginBottom:'10px'}}, 'Expected:'),
                RE.div({}, answerFromBE),
            )
        }
    }

    function getUserInputBackgroundColor() {
        if (hasValue(answerFromBE)) {
            return isUserInputCorrect() ? '#c6ebc6' : '#ffb3b3'
        }
    }

    function renderUserTranslation() {
        return RE.TextField({
            id: USER_INPUT_TEXT_FIELD,
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            autoFocus: true,
            value: userInput,
            label: 'Translation',
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            inputProps: {cols:24, tabIndex:1},
            style: {backgroundColor:getUserInputBackgroundColor()},
            onChange: event => {
                onUserInputChange({newUserInput:event.nativeEvent.target.value})
            },
            onKeyUp: event => {
                if (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) {
                    if (!event.shiftKey) {
                        validateTranslation()
                    } else if (hasValue(answerFromBE)) {
                        toggleShowAnswerButton()
                    }
                }
            },
        })
    }

    async function validateTranslation() {
        if (hasNoValue(beValidationResult) && !validationRequestIsInProgress) {
            if (userInput.trim().length === 0) {
                showMessage({text: 'Translation must not be empty.'})
            } else {
                setValidationRequestIsInProgress(true)
                const res = await be.validateTranslateCard({cardId, userProvidedTranslation: userInput})
                setValidationRequestIsInProgress(false)
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
        if (newUserInput.length === 1 && isUpperCase(newUserInput) && !isUpperCase(card.translation.charAt(0))) {
            newUserInput = newUserInput.toLowerCase()
        }
        if (newUserInput != userInput) {
            setUserInput(newUserInput)
        }
    }

    function renderDelay() {
        return RE.TextField({
            ref: delayTextField,
            id: CARD_DELAY_TEXT_FIELD,
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value: delay??'',
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 10,
            size: 'small',
            inputProps: {size:8, tabIndex:2},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != delay) {
                    setDelay(newText)
                }
            },
            onKeyUp: event => (event.keyCode === ENTER_KEY_CODE) ? updateSchedule() : null,
        })
    }

    async function updateSchedule() {
        setUpdateDelayRequestIsInProgress(true)
        const res = await be.updateTranslateCard({cardId, delay, recalculateDelay: true})
        setUpdateDelayRequestIsInProgress(false)
        if (res.err) {
            showError(res.err)
        } else {
            onDone()
        }
    }

    function renderCardsRemaining() {
        return RE.div({}, `Cards: ${cardsRemain}`)
    }

    function focusUserTranslation() {
        const userTextInput = document.getElementById(USER_INPUT_TEXT_FIELD)
        userTextInput?.focus()
        userTextInput?.scrollIntoView()
    }

    function toggleShowAnswerButton() {
        setAnswerFromBEIsShown(prev => !prev)
    }

    function renderValidateButton() {
        if (hasNoValue(answerFromBE)) {
            if (validationRequestIsInProgress) {
                return RE.span({}, RE.CircularProgress({size:24, style: {marginLeft: '5px'}}))
            } else {
                const disabled = hasValue(answerFromBE)
                return iconButton({
                    iconName:'send',
                    onClick: async () => {
                        if (!(await validateTranslation())) {
                            focusUserTranslation()
                        }
                    },
                    disabled,
                    iconStyle:{color:disabled?'lightgrey':'blue'}
                })
            }
        } else {
            return iconButton({
                iconName: answerFromBEIsShown ? 'visibility_off' : 'visibility',
                onClick: () => {
                    toggleShowAnswerButton()
                    focusUserTranslation()
                },
                iconStyle: {color: 'blue'}
            })
        }
    }

    function renderNextButton() {
        if (updateDelayRequestIsInProgress) {
            return RE.span({}, RE.CircularProgress({size:24, style: {marginLeft: '5px'}}))
        } else {
            return iconButton({
                iconName: 'done',
                onClick: updateSchedule,
                iconStyle: {color: 'blue'}
            })
        }
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
                onCancelled: () => setEditMode(false),
                onSaved: loadCard,
                onDeleted: onDone
            })
        } else {
            return RE.Container.col.top.left({},{style: {marginTop: '10px'}},
                renderQuestion(),
                RE.If(answerFromBEIsShown, renderExpectedTranslation),
                RE.Container.row.left.center({},{},
                    renderUserTranslation(),
                    renderValidateButton(),
                ),
                RE.If(hasValue(answerFromBE) && isUserInputCorrect(), () => RE.Container.row.left.center({},{},
                    RE.span({style:{marginRight:'10px'}}, card.timeSinceLastCheck),
                    renderDelay(),
                    renderNextButton(),
                )),
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        RE.If(controlsContainer?.current && !editMode, () => RE.Portal({container:controlsContainer.current},
            renderEditButton(),
            renderCardsRemaining()
        )),
        renderMessagePopup(),
    )

}

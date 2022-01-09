"use strict";

const RepeatTranslateCardCmp = ({allTags, allTagsMap, controlsContainer, cardToRepeat, onCardWasDeleted, onCardWasUpdated, onDone, cycledMode,
                                    delayCoefs, updateDelayCoefs,
                                    say, renderTextReaderConfig, setTextToSay}) => {
    const USER_INPUT_TEXT_FIELD = 'user-input'
    const CARD_DELAY_TEXT_FIELD = 'card-delay'

    const {renderMessagePopup, showError, showMessage} = useMessagePopup()

    const [card, setCard] = useState(cardToRepeat)
    const [errorLoadingCard, setErrorLoadingCard] = useState(null)
    const [userInput, setUserInput] = useState('')
    const [validationRequestIsInProgress, setValidationRequestIsInProgress] = useState(false)
    const [answerFromBE, setAnswerFromBE] = useState(null)
    const [answerFromBEIsShown, setAnswerFromBEIsShown] = useState(false)
    const [beValidationResult, setBeValidationResult] = useState(null)
    const [delay, setDelay] = useState(card?.schedule?.delay)
    const [autoFocusDelay, setAutoFocusDelay] = useState(false)
    const delayTextField = useRef(null)
    const [updateDelayRequestIsInProgress, setUpdateDelayRequestIsInProgress] = useState(false)

    const [editMode, setEditMode] = useState(false)
    const [cardWasUpdated, setCardWasUpdated] = useState(false)

    const {renderValidationHistory} = useTranslateCardHistory({cardId:cardToRepeat.id,tabIndex:3})

    const [textReaderConfigOpened, setTextReaderConfigOpened] = useState(false)

    useEffect(() => {
        if (autoFocusDelay && delayTextField.current) {
            const delayInput = document.getElementById(CARD_DELAY_TEXT_FIELD)
            delayInput?.focus?.()
            delayInput?.select?.()
            delayInput?.scrollIntoView?.()
            setAutoFocusDelay(false)
        }
    }, [delayTextField.current])

    async function reloadCard() {
        setCard(null)
        setDelay(null)
        setErrorLoadingCard(null)
        const resp = await be.readTranslateCardById({cardId:cardToRepeat.id})
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
    }

    function renderQuestion() {
        if (card) {
            let question
            if (card.textToTranslate.indexOf('\n') >= 0) {
                question = multilineTextToTable({text:card.textToTranslate})
            } else {
                question = RE.div({}, card.textToTranslate)
            }
            return RE.Container.col.top.left({},{},
                RE.div({style:{fontWeight:'bold',marginBottom:'10px'}}, 'Translate:'),
                question
            )
        }
    }

    function renderExpectedTranslation() {
        if (answerFromBE) {
            let expected
            if (answerFromBE.indexOf('\n') >= 0) {
                expected = multilineTextToTable({text:answerFromBE})
            } else {
                expected = RE.div({}, answerFromBE)
            }
            return RE.Container.col.top.left({},{},
                RE.div({style:{fontWeight:'bold',marginBottom:'10px'}}, 'Expected:',),
                expected,
                RE.If(window.speechSynthesis, () => RE.Container.row.left.center({},{},
                    iconButton({
                        iconName:'equalizer', onClick: () => {
                            if (!textReaderConfigOpened) {
                                setTextToSay(answerFromBE)
                            }
                            setTextReaderConfigOpened(prev => !prev)
                        }
                    }),
                    iconButton({iconName:'volume_up', onClick: () => say(answerFromBE)}),
                )),
                RE.If(textReaderConfigOpened, renderTextReaderConfig)
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
                const res = await be.validateTranslateCard({cardId:card.id, userProvidedTranslation: userInput})
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
        if (cycledMode) {
            return RE.Button({ref: delayTextField, id: CARD_DELAY_TEXT_FIELD, tabIndex:2, onClick: proceedToNextCard}, 'Next')
        } else {
            return re(DelayCmp,{
                actualDelay: card.timeSinceLastCheck,
                initialDelay:card?.schedule?.delay??'',
                delay:delay??'',
                delayOnChange: newDelay => setDelay(newDelay),
                coefs:delayCoefs,
                coefsOnChange:updateDelayCoefs,
                delayTextFieldRef:delayTextField,
                delayTextFieldId:CARD_DELAY_TEXT_FIELD,
                delayTextFieldTabIndex:2,
                updateDelayRequestIsInProgress,
                onSubmit: updateSchedule,
                onF9: () => say(answerFromBE)
            })
        }
    }

    async function updateSchedule() {
        setUpdateDelayRequestIsInProgress(true)
        const res = await be.updateTranslateCard({cardId:card.id, delay, recalculateDelay: true})
        setUpdateDelayRequestIsInProgress(false)
        if (res.err) {
            showError(res.err)
        } else {
            proceedToNextCard()
        }
    }

    function proceedToNextCard() {
        onDone({cardWasUpdated})
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

    function renderEditButton() {
        return iconButton({iconName:'edit', onClick: () => setEditMode(true)})
    }

    function renderPageContent() {
        if (editMode) {
            return re(EditTranslateCardCmp, {
                allTags,
                allTagsMap,
                card,
                reducedMode: hasNoValue(answerFromBE),
                onCancelled: () => setEditMode(false),
                onSaved: () => {
                    onCardWasUpdated()
                    setCardWasUpdated(true)
                    setEditMode(false)
                    reloadCard()
                },
                onDeleted: onCardWasDeleted
            })
        } else if (hasValue(errorLoadingCard)) {
            return `An error occurred during card loading: [${errorLoadingCard.code}] - ${errorLoadingCard.msg}`
        } else if (hasNoValue(card)) {
            return `Reloading card...`
        } else {
            return RE.Container.col.top.left({},{style: {marginBottom: '10px'}},
                renderQuestion(),
                RE.If(answerFromBEIsShown, renderExpectedTranslation),
                RE.Container.row.left.center({},{},
                    renderUserTranslation(),
                    renderValidateButton(),
                ),
                RE.If(hasValue(answerFromBE) && isUserInputCorrect(), () => RE.Container.col.top.left({},{},
                    renderDelay(),
                    renderValidationHistory()
                )),
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        RE.If(controlsContainer?.current && !editMode, () => RE.Portal({container:controlsContainer.current},
            renderEditButton()
        )),
        renderMessagePopup(),
    )

}

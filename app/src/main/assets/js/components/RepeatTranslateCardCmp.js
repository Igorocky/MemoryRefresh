"use strict";

const RepeatTranslateCardCmp = ({allTags, allTagsMap, controlsContainer, cardToRepeat, onCardWasDeleted, onCardWasUpdated, onDone, cycledMode,
                                    delayCoefs, updateDelayCoefs,
                                    defCoefs, updateDefCoefs,
                                    maxDelay, updateMaxDelay,
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
    const [autoFocusDelay, setAutoFocusDelay] = useState(false)
    const validateBtnRef = useRef(null)
    const delayTextField = useRef(null)
    const setDelayRef = useRef(null)
    const delayResultRef = useRef(null)
    const [updateDelayRequestIsInProgress, setUpdateDelayRequestIsInProgress] = useState(false)

    const [editMode, setEditMode] = useState(false)
    const [cardWasUpdated, setCardWasUpdated] = useState(false)

    const {renderValidationHistory} = useTranslateCardHistory({cardId:cardToRepeat.id,tabIndex:3})

    const [textReaderConfigOpened, setTextReaderConfigOpened] = useState(false)

    const userInputIsCorrect = hasValue(answerFromBE)
        && (answerFromBE === userInput.trim() || card?.direction === 'FOREIGN_NATIVE')

    useEffect(() => {
        if (autoFocusDelay && delayTextField.current) {
            const delayInput = document.getElementById(CARD_DELAY_TEXT_FIELD)
            delayInput?.focus?.()
            delayInput?.select?.()
            delayInput?.scrollIntoView?.()
            setAutoFocusDelay(false)
        }
    }, [delayTextField.current, autoFocusDelay])

    useEffect(() => {
        if (validateBtnRef.current && card?.direction === 'FOREIGN_NATIVE') {
            validateBtnRef.current.focus()
        }
    }, [validateBtnRef.current])

    async function reloadCard() {
        setCard(null)
        setErrorLoadingCard(null)
        const resp = await be.readTranslateCardById({cardId:cardToRepeat.id})
        if (resp.err) {
            await showError(resp.err)
            setErrorLoadingCard(resp.err)
        } else {
            const newCard = resp.data
            setCard(newCard)
            if (editMode && hasValue(answerFromBE)) {
                const newAnswerFromBe = newCard.direction === 'NATIVE_FOREIGN'
                    ? newCard.translation : newCard.textToTranslate
                setAnswerFromBE(newAnswerFromBe.trim())
            }
        }
    }

    function renderQuestion() {
        if (card) {
            const questionText = card.direction === 'NATIVE_FOREIGN' ? card.textToTranslate : card.translation
            let content = null
            if (questionText.indexOf('\n') >= 0) {
                content = multilineTextToTable({text:questionText, tableStyle:{marginTop:'10px'}})
            } else {
                content = RE.div({style:{border:'1px solid lightgrey', borderRadius:'5px', marginTop:'10px', padding:'3px'}},
                    questionText
                )
            }
            return RE.Paper({style:{backgroundColor: 'rgb(255, 249, 230)'}}, content)
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
                RE.If(card?.direction === 'NATIVE_FOREIGN' && window.speechSynthesis, () => RE.Container.row.left.center({},{},
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
            return userInputIsCorrect ? '#c6ebc6' : '#ffb3b3'
        }
    }

    function renderUserTranslation() {
        if (card?.direction === 'NATIVE_FOREIGN') {
            return textField({
                id: USER_INPUT_TEXT_FIELD,
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
                onKeyDown: event => {
                    if (event.keyCode === F9_KEY_CODE && hasValue(answerFromBE)) {
                        event.preventDefault();
                        say(answerFromBE)
                    } else {
                        const newDelay = getDelayCoefByKeyCode({event, coefs:delayCoefs, currDelay:delayResultRef.current, initialDelay:card?.schedule?.delay??''})
                        if (hasValue(newDelay) && setDelayRef.current) {
                            setDelayRef.current(newDelay)
                        }
                    }
                },
                onKeyUp: event => {
                    if (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) {
                        if (!event.shiftKey) {
                            validateTranslation()
                        } else if (hasValue(answerFromBE)) {
                            toggleShowAnswerButton()
                        }
                    } else if (event.altKey && event.keyCode === ENTER_KEY_CODE) {
                        if (userInputIsCorrect) {
                            if (cycledMode) {
                                proceedToNextCard()
                            } else if (hasValue(delayResultRef.current)) {
                                updateSchedule({delay:delayResultRef.current})
                            }
                        }
                    }
                },
            })
        }
    }

    async function validateTranslation() {
        if (hasNoValue(beValidationResult) && !validationRequestIsInProgress) {
            if (card?.direction === 'NATIVE_FOREIGN' && userInput.trim().length === 0) {
                showMessage({text: 'Translation must not be empty.'})
            } else {
                setValidationRequestIsInProgress(true)
                const res = await be.validateTranslateCard({cardId:card.id, userProvidedTranslation: userInput.trim()})
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
                    if (card?.direction === 'FOREIGN_NATIVE') {
                        toggleShowAnswerButton()
                    }
                    return res.data.isCorrect
                }
            }
        }
    }

    function onUserInputChange({newUserInput}) {
        if (newUserInput !== userInput) {
            setUserInput(newUserInput)
        }
    }

    function renderDelay() {
        if (cycledMode) {
            return RE.Button({
                ref: delayTextField,
                id: CARD_DELAY_TEXT_FIELD,
                tabIndex:2,
                onClick: proceedToNextCard,
                onKeyUp: event => {
                    if (event.keyCode === F9_KEY_CODE) {
                        event.preventDefault();
                        say(answerFromBE)
                    }
                }
            }, 'Next')
        } else {
            return re(DelayCmp,{
                beValidationResult,
                actualDelay: card.timeSinceLastCheck,
                initialDelay:card?.schedule?.delay??'',
                setDelayRef,
                delayResultRef,
                coefs:delayCoefs,
                coefsOnChange:updateDelayCoefs,
                defCoefs,
                updateDefCoefs,
                maxDelay,
                maxDelayOnChange:updateMaxDelay,
                delayTextFieldRef:delayTextField,
                delayTextFieldId:CARD_DELAY_TEXT_FIELD,
                delayTextFieldTabIndex:2,
                updateDelayRequestIsInProgress,
                onSubmit: delay => updateSchedule({delay}),
                onF9: () => say(answerFromBE)
            })
        }
    }

    async function updateSchedule({delay}) {
        if (userInputIsCorrect) {
            setUpdateDelayRequestIsInProgress(true)
            const res = await be.updateTranslateCard({cardId:card.id, delay, recalculateDelay: true})
            setUpdateDelayRequestIsInProgress(false)
            if (res.err) {
                showError(res.err)
            } else {
                proceedToNextCard()
            }
        }
    }

    function proceedToNextCard() {
        if (userInputIsCorrect) {
            onDone({cardWasUpdated})
        }
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
                async function validateAnswer() {
                    if (!(await validateTranslation())) {
                        focusUserTranslation()
                    }
                }
                return iconButton({
                    ref: validateBtnRef,
                    iconName:card?.direction === 'NATIVE_FOREIGN'?'send':'visibility',
                    onClick: validateAnswer,
                    onKeyDown: event => {
                        event.preventDefault();
                    },
                    onKeyUp: event => {
                        if (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) {
                            validateAnswer()
                        }
                    },
                    style: {marginLeft: card?.direction === 'FOREIGN_NATIVE'?'140px':null},
                    iconStyle: {color: 'blue'}
                })
            }
        } else if (card?.direction === 'NATIVE_FOREIGN') {
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
                reducedMode:
                    (card?.direction === 'NATIVE_FOREIGN' && hasNoValue(answerFromBE))
                    || (card?.direction === 'FOREIGN_NATIVE' && !answerFromBEIsShown),
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
                RE.If(
                    hasValue(answerFromBE) && userInputIsCorrect,
                    () => RE.Container.col.top.left({},{},
                        renderDelay(),
                        renderValidationHistory(),
                    )
                ),
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

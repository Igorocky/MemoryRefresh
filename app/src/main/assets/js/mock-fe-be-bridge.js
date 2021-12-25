'use strict';

function createFeBeBridgeForUiTestMode() {
    const mockedBeFunctions = {}

    function okResponse(data) {
        return {
            data,
            mapData: mapper => okResponse(mapper(data))
        }
    }

    function errResponse(errCode, msg) {
        return {
            err: {code:errCode,msg},
            mapData() {
                return this
            }
        }
    }

    const CARDS = []

    mockedBeFunctions.saveNewTranslateCard = ({textToTranslate, translation}) => {
        textToTranslate = textToTranslate?.trim()??''
        translation = translation?.trim()??''
        if (textToTranslate == '') {
            return errResponse(1, 'textToTranslate is empty')
        } else if (translation == '') {
            return errResponse(2, 'translation is empty')
        } else {
            const id = (CARDS.map(c=>c.id).max()??0)+1
            const newCard = {
                id,
                textToTranslate,
                translation,
                timeSinceLastCheck: '1d 3h',
                schedule: {
                    cardId: id,
                    delay: '0m',
                    nextAccessInMillis: 0,
                    nextAccessAt: new Date().getTime()
                }
            }
            CARDS.push(newCard)
            return okResponse(newCard)
        }
    }

    mockedBeFunctions.getTranslateCardById = ({cardId}) => {
        const card = CARDS.find(c=>c.id==cardId)
        if (hasNoValue(card)) {
            return errResponse(9, 'Error getting translate card by id.')
        } else {
            return okResponse({
                id: card.id,
                textToTranslate: card.textToTranslate,
                translation: card.translation,
                timeSinceLastCheck: card.timeSinceLastCheck,
                schedule: {
                    cardId: card.schedule.cardId,
                    delay: card.schedule.delay,
                    nextAccessInMillis: card.schedule.nextAccessInMillis,
                    nextAccessAt: card.schedule.nextAccessAt,
                }
            })
        }
    }

    mockedBeFunctions.validateTranslateCard = ({cardId, userProvidedTranslation}) => {
        const card = CARDS.find(c=>c.id==cardId)
        if (hasNoValue(card)) {
            return errResponse(11, 'Error getting translate card by id.')
        } else {
            return okResponse({
                answer: card.translation,
                isCorrect: card.translation == userProvidedTranslation
            })
        }
    }

    mockedBeFunctions.updateTranslateCard = ({cardId, textToTranslate, translation, delay, recalculateDelay}) => {
        const card = CARDS.find(c=>c.id==cardId)
        if (hasNoValue(card)) {
            return errResponse(7, 'Error getting translate card by id.')
        } else {
            card.textToTranslate = textToTranslate??card.textToTranslate
            card.translation = translation??card.translation
            if (hasValue(delay) && (delay != card.schedule.delay || recalculateDelay)) {
                card.schedule.delay = delay
                card.schedule.nextAccessInMillis = 1000
                card.schedule.nextAccessAt = (new Date().getTime()) + card.schedule.nextAccessInMillis
            }
            return okResponse({
                id: card.id,
                textToTranslate: card.textToTranslate,
                translation: card.translation,
                timeSinceLastCheck: card.timeSinceLastCheck,
                schedule: {
                    cardId: card.schedule.cardId,
                    delay: card.schedule.delay,
                    nextAccessInMillis: card.schedule.nextAccessInMillis,
                    nextAccessAt: card.schedule.nextAccessAt,
                }
            })
        }
    }

    mockedBeFunctions.getNextCardToRepeat = () => {
        if (!CARDS.length) {
            return okResponse({cardsRemain: 0, nextCardIn: ''})
        } else {
            const curTime = new Date().getTime()
            const activeCards = CARDS.filter(c=>c.schedule.nextAccessAt <= curTime)
            if (!activeCards.length) {
                return okResponse({cardsRemain: 0, nextCardIn: '###'})
            } else {
                const selectedCard = activeCards[randomInt(0,activeCards.length-1)]
                return okResponse({
                    cardId: selectedCard.id,
                    cardType: 'TRANSLATION',
                    cardsRemain: activeCards.length,
                    isCardsRemainExact: true
                })
            }
        }
    }

    mockedBeFunctions.deleteTranslateCard = ({cardId}) => {
        removeIf(CARDS, c=>c.id===cardId)
        return okResponse(true)
    }

    mockedBeFunctions.doBackup = () => {
        return okResponse({name:'new-backup-' + new Date(), size:4335})
    }

    mockedBeFunctions.listAvailableBackups = () => {
        return okResponse([
            {name:'backup-1', size:1122},
            {name:'backup-2', size:456456},
            {name:'backup-3', size:998877},
        ])
    }

    mockedBeFunctions.restoreFromBackup = ({backupName}) => {
        return okResponse(`The database was restored from the backup ${backupName}`)
    }

    mockedBeFunctions.deleteBackup = async ({backupName}) => {
        return await mockedBeFunctions.listAvailableBackups()
    }

    mockedBeFunctions.shareBackup = ({backupName}) => {
        return okResponse({})
    }

    mockedBeFunctions.startHttpServer = () => {
        return okResponse({})
    }

    mockedBeFunctions.getSharedFileInfo = () => {
        return okResponse({name: 'shared-file-name-111', uri: 'file://shared-file-name-111', type: 'BACKUP'})
    }

    mockedBeFunctions.closeSharedFileReceiver = () => {
        return okResponse({})
    }

    mockedBeFunctions.saveSharedFile = () => {
        return okResponse(12)
    }

    const HTTP_SERVER_STATE = {
        isRunning: false,
        url: "URL",
        settings: {
            keyStoreName: '---keyStoreName---',
            keyStorePassword: '---keyStorePassword---',
            keyAlias: '---keyAlias---',
            privateKeyPassword: '---privateKeyPassword---',
            port: 8443,
            serverPassword: '---serverPassword---',
        }
    }
    mockedBeFunctions.getHttpServerState = () => {
        return okResponse({...HTTP_SERVER_STATE})
    }
    mockedBeFunctions.saveHttpServerSettings = (settings) => {
        HTTP_SERVER_STATE.settings = settings
        return mockedBeFunctions.getHttpServerState()
    }
    mockedBeFunctions.startHttpServer = () => {
        HTTP_SERVER_STATE.isRunning = true
        return mockedBeFunctions.getHttpServerState()
    }
    mockedBeFunctions.stopHttpServer = () => {
        HTTP_SERVER_STATE.isRunning = false
        return mockedBeFunctions.getHttpServerState()
    }

    function fillDbWithMockData() {
        mockedBeFunctions.saveNewTranslateCard({textToTranslate:'A', translation:'a'})
        mockedBeFunctions.saveNewTranslateCard({textToTranslate:'B', translation:'b'})
        mockedBeFunctions.saveNewTranslateCard({textToTranslate:'C', translation:'c'})
    }
    fillDbWithMockData()

    function createBeFunction(funcName, delay) {
        const beFunc = mockedBeFunctions[funcName]
        if (hasNoValue(beFunc)) {
            console.error(`mocked backend function is not defined - ${funcName}`)
        }
        return function (arg) {
            return new Promise((resolve,reject) => {
                const doResolve = () => resolve(beFunc(arg))
                if (hasValue(delay)) {
                    setTimeout(doResolve, delay)
                } else {
                    doResolve()
                }
            })
        }
    }

    return {
        createBeFunction,
        feBeBridgeState: {mockedBeFunctions},
    }
}

const mockFeBeBridge = createFeBeBridgeForUiTestMode()

const createBeFunction = mockFeBeBridge.createBeFunction
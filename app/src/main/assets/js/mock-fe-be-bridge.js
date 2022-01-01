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
    const TAGS = []
    const CARDS_TO_TAGS = []

    mockedBeFunctions.createTag = ({name}) => {
        if (TAGS.find(t=>t.name==name)) {
            return errResponse(36,`'${name}' tag already exists.`)
        } else {
            const id = (TAGS.map(t=>t.id).max()??0)+1
            const newTag = {id,name,createdAt:new Date().getTime()}
            TAGS.push(newTag)
            return okResponse(id)
        }
    }

    mockedBeFunctions.readAllTags = () => {
        return okResponse(TAGS.map(t => ({...t})))
    }

    mockedBeFunctions.getCardToTagMapping = () => {
        const res = {}
        CARDS_TO_TAGS.forEach(({cardId,tagId}) => {
            if (res[cardId] === undefined) {
                res[cardId] = []
            }
            res[cardId].push(tagId)
        })
        return okResponse(res)
    }

    mockedBeFunctions.updateTag = ({tagId,name}) => {
        const tagsToUpdate = TAGS.filter(t=>t.id === tagId)
        let updatedTag = null
        for (const tag of tagsToUpdate) {
            if (TAGS.find(t=> t.name === name && t.id !== tagId)) {
                return errResponse(1, `'${name}' tag already exists.`)
            } else {
                tag.name = name
                updatedTag = tag
            }
        }
        if (updatedTag == null) {
            return errResponse(1, `updatedTag == null`)
        }
        return okResponse(updatedTag)
    }

    mockedBeFunctions.deleteTag = ({tagId}) => {
        // return errResponse(2,'Error while deleting a tag.')
        if (CARDS_TO_TAGS.find(({cardId,tagId}) => tagId===tagId)) {
            return errResponse(222,'This tag is used by a card.')
        } else {
            removeIf(TAGS,t => t.id===tagId)
            return okResponse()
        }
    }

    mockedBeFunctions.createTranslateCard = ({textToTranslate, translation, tagIds, paused}) => {
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
                createdAt: new Date().getTime(),
                paused,
                schedule: {
                    cardId: id,
                    updatedAt: new Date().getTime(),
                    delay: '0m',
                    nextAccessInMillis: 0,
                    nextAccessAt: new Date().getTime()
                },
                timeSinceLastCheck: '1d 3h',
                overdue: 1.03,
                textToTranslate,
                translation
            }
            CARDS.push(newCard)
            for (let tagId of tagIds) {
                CARDS_TO_TAGS.push({cardId:id,tagId})
            }
            return okResponse(id)
        }
    }

    mockedBeFunctions.readTranslateCardById = ({cardId}) => {
        const card = CARDS.find(c=>c.id==cardId)
        if (hasNoValue(card)) {
            return errResponse(9, 'Error getting translate card by id.')
        } else {
            return okResponse({
                id: card.id,
                createdAt: card.createdAt,
                paused: card.paused,
                tagIds: CARDS_TO_TAGS.filter(p => p.cardId === cardId).map(p=>p.tagId),
                schedule: {
                    cardId: card.schedule.cardId,
                    updatedAt: card.schedule.updatedAt,
                    delay: card.schedule.delay,
                    nextAccessInMillis: card.schedule.nextAccessInMillis,
                    nextAccessAt: card.schedule.nextAccessAt,
                },
                timeSinceLastCheck: card.timeSinceLastCheck,
                overdue: card.overdue,
                textToTranslate: card.textToTranslate,
                translation: card.translation,
            })
        }
    }

    mockedBeFunctions.readTranslateCardsByFilter = (filter) => {
        console.log('filter', filter)
        // return okResponse([])
        return okResponse(CARDS.map(c=>mockedBeFunctions.readTranslateCardById({cardId:c.id}).data))
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
        const numOfTags = 50
        ints(1,numOfTags)
            .map(i=>randomAlphaNumString({minLength:3,maxLength:5}))
            .forEach(s=>mockedBeFunctions.createTag({name:s}))

        function getRandomTagIds() {
            let numOfTags = randomInt(1,5)
            let result = []
            while (result.length < numOfTags) {
                let newId = TAGS[randomInt(0,TAGS.length-1)].id
                if (!result.includes(newId)) {
                    result.push(newId)
                }
            }
            return result
        }

        const numOfCards = 1_000
        ints(1,numOfCards)
            .map(i=>randomSentence({minLength: 1, maxLength: 3}))
            .forEach(s=>mockedBeFunctions.createTranslateCard({
                textToTranslate:s.toUpperCase(),
                translation:s.toLowerCase(),
                tagIds:getRandomTagIds(),
                paused: randomInt(0,1) === 1
            }))
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
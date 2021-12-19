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

    const TAGS = []
    const NOTES = []
    const NOTES_TO_TAGS = []

    mockedBeFunctions.saveNewTag = ({name}) => {
        if (TAGS.find(t=>t.name==name)) {
            return errResponse(1,`'${name}' tag already exists.`)
        } else {
            const id = (TAGS.map(t=>t.id).max()??0)+1
            const newTag = {id,name,createdAt:new Date().getTime()}
            TAGS.push(newTag)
            return okResponse(newTag)
        }
    }

    mockedBeFunctions.getTags = () => {
        return okResponse(TAGS.map(t => ({...t})))
    }

    mockedBeFunctions.updateTag = ({id,name}) => {
        const tagsToUpdate = TAGS.filter(t=>t.id==id)
        for (const tag of tagsToUpdate) {
            if (TAGS.find(t=> t.name==name && t.id != id)) {
                return errResponse(1, `'${name}' tag already exists.`)
            } else {
                tag.name = name
            }
        }
        return okResponse(tagsToUpdate.length)
    }

    mockedBeFunctions.deleteTag = ({id}) => {
        // return errResponse(2,'Error while deleting a tag.')
        if (NOTES_TO_TAGS.find(({noteId,tagId}) => tagId==id)) {
            return errResponse(222,'This tag is used by a note.')
        } else {
            return okResponse(removeIf(TAGS,t => t.id==id))
        }
    }

    mockedBeFunctions.saveNewNote = ({text, tagIds}) => {
        const id = (NOTES.map(n=>n.id).max()??0)+1
        const newNote = {id,text,createdAt:new Date().getTime()}
        NOTES.push(newNote)
        for (let tagId of tagIds) {
            NOTES_TO_TAGS.push({noteId:id,tagId})
        }
        return okResponse(newNote)
    }

    mockedBeFunctions.getNotes = ({tagIdsToInclude=[],tagIdsToExclude=[],searchInDeleted = false}) => {
        function getAllTagIdsOfNote({noteId}) {
            return NOTES_TO_TAGS
                .filter(({noteId:id,tagId})=>noteId==id)
                .map(({tagId})=>tagId)
        }
        function hasTags({noteId,tagIds,atLeastOne = false}) {
            let noteTagIds = getAllTagIdsOfNote({noteId})
            if (atLeastOne) {
                return hasValue(tagIds.find(id => noteTagIds.includes(id)))
            } else {
                return tagIds.length && tagIds.every(id => noteTagIds.includes(id))
            }
        }
        let result = NOTES
            .filter(note => searchInDeleted && note.isDeleted || !searchInDeleted && !note.isDeleted)
            .filter(note => tagIdsToInclude.length == 0 || hasTags({noteId:note.id,tagIds:tagIdsToInclude}))
            .filter(note => tagIdsToExclude.length == 0 || !hasTags({noteId:note.id, tagIds:tagIdsToExclude, atLeastOne:true}))
            .map(note => ({...note, tagIds:getAllTagIdsOfNote({noteId:note.id})}))
        return okResponse({items:result,complete:true})
    }

    mockedBeFunctions.getRemainingTagIds = (args) => {
        return mockedBeFunctions.getNotes(args).mapData(({items}) => items.flatMap(n => n.tagIds).distinct())
    }

    mockedBeFunctions.updateNote = ({id,text,tagIds,isDeleted}) => {
        const notesToUpdate = NOTES.filter(n=>n.id==id)
        for (const note of notesToUpdate) {
            if (hasValue(text)) {
                note.text = text
            }
            if (hasValue(tagIds)) {
                removeIf(NOTES_TO_TAGS, ({noteId}) => noteId == id)
                for (let tagId of tagIds) {
                    NOTES_TO_TAGS.push({noteId:note.id,tagId})
                }
            }
            if (hasValue(isDeleted)) {
                note.isDeleted = isDeleted
            }
        }
        return okResponse(notesToUpdate.length)
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
        const numOfTags = 30
        const tags = ints(1,numOfTags)
            .map(i=>randomAlphaNumString({minLength:3,maxLength:5}))
            .map(s=>mockedBeFunctions.saveNewTag({name:s}))
            .map(({data:tag}) => tag)

        function getRandomTagIds() {
            let numOfTags = randomInt(1,5)
            let result = []
            while (result.length < numOfTags) {
                let newId = tags[randomInt(0,tags.length-1)].id
                if (!result.includes(newId)) {
                    result.push(newId)
                }
            }
            return result
        }

        const numOfNotes = 500
        const notes = ints(1,numOfNotes)
            .map(i=>randomSentence({}))
            .map(s=>mockedBeFunctions.saveNewNote({text:s, tagIds:getRandomTagIds()}))
            .map(({data:note}) => note)
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
        feBeBridgeState: {mockedBeFunctions, TAGS, NOTES, NOTES_TO_TAGS},
    }
}

const mockFeBeBridge = createFeBeBridgeForUiTestMode()

const createBeFunction = mockFeBeBridge.createBeFunction
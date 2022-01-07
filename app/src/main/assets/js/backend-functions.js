'use strict';

const be = {
    doBackup: createBeFunction('doBackup'),
    listAvailableBackups: createBeFunction('listAvailableBackups'),
    restoreFromBackup: createBeFunction('restoreFromBackup'),
    deleteBackup: createBeFunction('deleteBackup'),
    shareBackup: createBeFunction('shareBackup'),

    getHttpServerState: createBeFunction('getHttpServerState'),
    saveHttpServerSettings: createBeFunction('saveHttpServerSettings'),
    startHttpServer: createBeFunction('startHttpServer'),
    stopHttpServer: createBeFunction('stopHttpServer'),

    getSharedFileInfo: createBeFunction('getSharedFileInfo'),
    closeSharedFileReceiver: createBeFunction('closeSharedFileReceiver'),
    saveSharedFile: createBeFunction('saveSharedFile'),

    createTag: createBeFunction('createTag'),
    readAllTags: createBeFunction('readAllTags'),
    getCardToTagMapping: createBeFunction('getCardToTagMapping'),
    updateTag: createBeFunction('updateTag'),
    deleteTag: createBeFunction('deleteTag'),
    createTranslateCard: createBeFunction('createTranslateCard'),
    readTranslateCardById: createBeFunction('readTranslateCardById'),
    readTranslateCardsByFilter: createBeFunction('readTranslateCardsByFilter'),
    selectTopOverdueTranslateCards: createBeFunction('selectTopOverdueTranslateCards'),
    readTranslateCardHistory: createBeFunction('readTranslateCardHistory'),
    validateTranslateCard: createBeFunction('validateTranslateCard'),
    updateTranslateCard: createBeFunction('updateTranslateCard'),
    deleteTranslateCard: createBeFunction('deleteTranslateCard'),
}


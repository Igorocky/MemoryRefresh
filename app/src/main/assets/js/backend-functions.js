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

    saveNewTranslateCard: createBeFunction('saveNewTranslateCard'),
    getTranslateCardById: createBeFunction('getTranslateCardById'),
    validateTranslateCard: createBeFunction('validateTranslateCard'),
    updateTranslateCard: createBeFunction('updateTranslateCard'),
    getNextCardToRepeat: createBeFunction('getNextCardToRepeat'),
    deleteTranslateCard: createBeFunction('deleteTranslateCard'),
}


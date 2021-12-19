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

    saveNewTag: createBeFunction('saveNewTag'),
    getAllTags: createBeFunction('getTags'),
    getRemainingTagIds: createBeFunction('getRemainingTagIds'),
    updateTag: createBeFunction('updateTag'),
    deleteTag: createBeFunction('deleteTag'),
    saveNewNote: createBeFunction('saveNewNote'),
    getNotes: createBeFunction('getNotes'),
    updateNote: createBeFunction('updateNote'),

}


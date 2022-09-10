"use strict";

const BackupsView = ({query,openView,setPageTitle}) => {

    const [allBackups, setAllBackups] = useState(null)
    const [focusedBackup, setFocusedBackup] = useState(null)

    const {renderMessagePopup, showMessage, confirmAction, showMessageWithProgress} = useMessagePopup()
    const {renderMessagePopup:renderMessagePopup2, showError} = useMessagePopup()

    useEffect(async () => {
        const {data:allBackups} = await be.listAvailableBackups()
        setAllBackups(allBackups)
    }, [])

    function renderAllBackups() {
        if (hasNoValue(allBackups)) {
            return 'Loading...'
        } else if (allBackups.length == 0) {
            return 'There are no backups.'
        } else {
            return RE.table({},
                RE.tbody({},
                    allBackups.map(backup =>
                        RE.tr({key:backup.name, onClick: () => setFocusedBackup(prev=> prev?.name==backup.name?null:backup), style:{backgroundColor: focusedBackup?.name == backup.name ? 'lightgrey' : undefined}},
                            RE.td({}, renderBackup({backup})),
                            RE.td({},
                                focusedBackup?.name === backup.name
                                    ? iconButton({iconName:'delete', onClick: () => deleteBackup({backup})})
                                    : null
                            ),
                            RE.td({},
                                focusedBackup?.name === backup.name
                                    ? iconButton({iconName:'ios_share', onClick: () => restoreFromBackup({backup})})
                                    : null
                            ),
                            RE.If(IS_IN_WEBVIEW, () => RE.td({},
                                focusedBackup?.name === backup.name
                                    ? iconButton({iconName:'share', onClick: () => shareBackup({backupName:backup.name})})
                                    : null
                            )),
                        )
                    )
                )
            )
        }
    }

    function renderBackup({backup}) {
        return `${backup.name} [${backup.size}]`
    }

    async function deleteBackup({backup}) {
        if (await confirmAction({text: `Confirm deleting backup '${backup.name}'`})) {
            const res = await be.deleteBackup({backupName: backup.name})
            if (!res.err) {
                setAllBackups(res.data)
                setFocusedBackup(null)
            } else {
                showError(res.err)
            }
        }
    }

    async function restoreFromBackup({backup}) {
        if (await confirmAction({text: `Confirm restoring from backup '${backup.name}'`})) {
            const closeProgressWindow = showMessageWithProgress({text: `Restoring from backup '${backup.name}'...`})
            const res = await be.restoreFromBackup({backupName: backup.name})
            closeProgressWindow()
            if (!res.err) {
                await showMessage({text: res.data})
            } else {
                showError(res.err)
            }
        }
    }

    async function shareBackup({backupName}) {
        const res = await be.shareFile({fileType: 'BACKUP', fileName: backupName})
        if (res.err) {
            showError(res.err)
        }
    }

    async function doBackup() {
        if (await confirmAction({text: `Confirm creating a backup?`})) {
            const closeProgressWindow = showMessageWithProgress({text: `Creating a backup...`})
            const res = await be.doBackup()
            closeProgressWindow()
            if (!res.err) {
                const newBackupName = res.data.name
                await showMessage({
                    text: `A backup was created '${newBackupName}'`,
                    additionalActionsRenderer: () => IS_IN_WEBVIEW
                        ? iconButton({iconName:'share', onClick: () => shareBackup({backupName:newBackupName})})
                        : null
                })
                setAllBackups(prev => [res.data, ...prev])
                setFocusedBackup(null)
            } else {
                showError(res.err)
            }
        }
    }

    return RE.Container.col.top.left({style:{marginTop:'5px'}},{},
        RE.Button({variant:"contained", color:'primary', onClick: doBackup}, 'Backup'),
        renderAllBackups(),
        renderMessagePopup(),
        renderMessagePopup2(),
    )
}

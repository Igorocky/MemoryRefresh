'use strict';

const ALL_MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December',]

const DateSelector = ({selectedDate, onDateSelected, minimized = false}) => {

    function pad2(val) {
        return val.toString().padStart(2, '0')
    }

    function getSelectedYear() {
        return selectedDate.getFullYear()
    }

    function getSelectedMonth() {
        return selectedDate.getMonth()
    }

    function getSelectedDayOfMonth() {
        return selectedDate.getDate()
    }

    function getSelectedHours() {
        return selectedDate.getHours()
    }

    function getSelectedMinutes() {
        return selectedDate.getMinutes()
    }

    function getSelectedSeconds() {
        return selectedDate.getSeconds()
    }

    function getNumberOfDaysInMonth(year, month) {
        return new Date(year, (month+1)%12, 0).getDate()
    }

    function yearSelected(year) {
        const numberOfDaysInMonth = getNumberOfDaysInMonth(year, getSelectedMonth())
        onDateSelected(new Date(
            year, getSelectedMonth(), Math.min(getSelectedDayOfMonth(), numberOfDaysInMonth),
            getSelectedHours(), getSelectedMinutes(), getSelectedSeconds()
        ))
    }

    function monthSelected(month) {
        const numberOfDaysInMonth = getNumberOfDaysInMonth(getSelectedYear(), month)
        onDateSelected(new Date(
            getSelectedYear(), month, Math.min(getSelectedDayOfMonth(), numberOfDaysInMonth),
            getSelectedHours(), getSelectedMinutes(), getSelectedSeconds()
        ))
    }

    function dayOfMonthSelected(day) {
        onDateSelected(new Date(
            getSelectedYear(), getSelectedMonth(), day,
            getSelectedHours(), getSelectedMinutes(), getSelectedSeconds()
        ))
    }

    function hoursSelected(hours) {
        onDateSelected(new Date(
            getSelectedYear(), getSelectedMonth(), getSelectedDayOfMonth(),
            hours, getSelectedMinutes(), getSelectedSeconds()
        ))
    }

    function minutesSelected(minutes) {
        onDateSelected(new Date(
            getSelectedYear(), getSelectedMonth(), getSelectedDayOfMonth(),
            getSelectedHours(), minutes, getSelectedSeconds()
        ))
    }

    function secondsSelected(seconds) {
        onDateSelected(new Date(
            getSelectedYear(), getSelectedMonth(), getSelectedDayOfMonth(),
            getSelectedHours(), getSelectedMinutes(), seconds
        ))
    }

    if (!minimized) {
        return RE.Container.col.top.left({},{},
            RE.Container.row.left.center({},{style:{margin:'5px'}},
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'year-select'}, 'Year'),
                    RE.Select(
                        {
                            value:getSelectedYear(),
                            variant: 'outlined',
                            label: 'Year',
                            labelId: 'year-select',
                            onChange: event => {
                                const newYear = event.target.value
                                yearSelected(newYear)
                            }
                        },
                        ints(2021,2031).map(idx => RE.MenuItem({key:idx, value:idx}, idx))
                    )
                ),
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'month-select'}, 'Month'),
                    RE.Select(
                        {
                            value:getSelectedMonth(),
                            variant: 'outlined',
                            label: 'Month',
                            labelId: 'month-select',
                            onChange: event => {
                                const newMonth = event.target.value
                                monthSelected(newMonth)
                            }
                        },
                        ints(0,11).map(idx => RE.MenuItem({key:idx, value:idx}, ALL_MONTHS[idx]))
                    )
                ),
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'day-select'}, 'Day'),
                    RE.Select(
                        {
                            value:getSelectedDayOfMonth(),
                            variant: 'outlined',
                            label: 'Day',
                            labelId: 'day-select',
                            onChange: event => {
                                const newDay = event.target.value
                                dayOfMonthSelected(newDay)
                            }
                        },
                        ints(1,getNumberOfDaysInMonth(getSelectedYear(),getSelectedMonth())).map(idx => RE.MenuItem({key:idx, value:idx}, idx))
                    )
                ),
            ),
            RE.Container.row.left.center({},{style:{margin:'5px'}},
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'hours-select'}, 'Hours'),
                    RE.Select(
                        {
                            value:getSelectedHours(),
                            variant: 'outlined',
                            label: 'Hours',
                            labelId: 'hours-select',
                            onChange: event => {
                                const newHours = event.target.value
                                hoursSelected(newHours)
                            }
                        },
                        ints(0,23).map(idx => RE.MenuItem({key:idx, value:idx}, pad2(idx)))
                    )
                ),
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'minutes-select'}, 'Minutes'),
                    RE.Select(
                        {
                            value:getSelectedMinutes(),
                            variant: 'outlined',
                            label: 'Minutes',
                            labelId: 'minutes-select',
                            onChange: event => {
                                const newMinutes = event.target.value
                                minutesSelected(newMinutes)
                            }
                        },
                        ints(0,59).map(idx => RE.MenuItem({key:idx, value:idx}, pad2(idx)))
                    )
                ),
                RE.FormControl({variant:"outlined"},
                    RE.InputLabel({id:'seconds-select'}, 'Seconds'),
                    RE.Select(
                        {
                            value:getSelectedSeconds(),
                            variant: 'outlined',
                            label: 'Seconds',
                            labelId: 'seconds-select',
                            onChange: event => {
                                const newSeconds = event.target.value
                                secondsSelected(newSeconds)
                            }
                        },
                        ints(0,59).map(idx => RE.MenuItem({key:idx, value:idx}, pad2(idx)))
                    )
                ),
            )
        )
    } else {
        return null
    }
}
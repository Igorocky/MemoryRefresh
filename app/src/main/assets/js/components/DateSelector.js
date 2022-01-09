'use strict';

const ALL_MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December',]

const DateSelector = ({selectedDate, onDateSelected, minimized = false}) => {

    function getSelectedYear() {
        return selectedDate.getFullYear()
    }

    function getSelectedMonth() {
        return selectedDate.getMonth()
    }

    function getSelectedDayOfMonth() {
        return selectedDate.getDate()
    }

    function getNumberOfDaysInMonth(year, month) {
        return new Date(year, (month+1)%12, 0).getDate()
    }

    function yearSelected(year) {
        const numberOfDaysInMonth = getNumberOfDaysInMonth(year, getSelectedMonth())
        onDateSelected(new Date(year, getSelectedMonth(), Math.min(getSelectedDayOfMonth(), numberOfDaysInMonth)))
    }

    function monthSelected(month) {
        const numberOfDaysInMonth = getNumberOfDaysInMonth(getSelectedYear(), month)
        onDateSelected(new Date(getSelectedYear(), month, Math.min(getSelectedDayOfMonth(), numberOfDaysInMonth)))
    }

    function dayOfMonthSelected(day) {
        onDateSelected(new Date(getSelectedYear(), getSelectedMonth(), day))
    }

    if (minimized) {
        return RE.span({style: {marginLeft: '5px', color:'blue'}},`${getSelectedYear()} ${ALL_MONTHS[getSelectedMonth()]} ${getSelectedDayOfMonth()}`)
    } else {
        return RE.Container.row.left.center({},{style:{margin:'5px'}},
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
        )
    }
}
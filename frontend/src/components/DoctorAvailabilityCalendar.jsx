import { useState, useMemo } from 'react';
import { createBatchAvailability } from '../services/appointmentApi';
import {
  toDateKey,
  formatTimeOnly,
  formatDateOnly,
  toIsoFromDateAndTime,
} from '../utils/dateTimeUtils';
import './DoctorAvailabilityCalendar.css';

const DAYS_OF_WEEK = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/**
 * Calendar-based Doctor Availability Planner.
 * Allows doctors to navigate monthly consultation schedules, inspect daily slots,
 * and publish new consultation availability intervals for selected dates.
 *
 * @param {Object} props
 * @param {Array} props.availabilities - Doctor's existing availability slots from API
 * @param {Function} props.onRefresh - Callback to refresh doctor availability data
 * @param {Function} props.onDeactivateSlot - Handler to deactivate a specific slot ID
 * @param {string|null} props.actionLoadingKey - Identifier of currently executing action
 * @param {string} props.token - In-memory JWT access token
 */
export default function DoctorAvailabilityCalendar({
  availabilities = [],
  onRefresh,
  onDeactivateSlot,
  actionLoadingKey,
  token,
}) {
  // Today's date key initialized safely on mount
  const [todayKey] = useState(() => toDateKey(new Date()));

  // Calendar month state (defaults to current date)
  const [viewDate, setViewDate] = useState(() => new Date());

  // Selected date key ("YYYY-MM-DD")
  const [selectedDate, setSelectedDate] = useState(() => {
    // Default to tomorrow or today
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return toDateKey(tomorrow);
  });

  // Multi-slot pre-scheduling builder state for selected date
  const [slotsToSchedule, setSlotsToSchedule] = useState(() => [
    { id: 1, startTime: '09:00', endTime: '13:00' },
  ]);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState(null);
  const [formSuccess, setFormSuccess] = useState(null);
  const [batchErrors, setBatchErrors] = useState([]);

  // Group doctor's availability slots by local date key ("YYYY-MM-DD")
  const slotsByDate = useMemo(() => {
    const map = new Map();
    for (const slot of availabilities) {
      if (!slot?.startTime) continue;
      const key = toDateKey(slot.startTime);
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key).push(slot);
    }
    // Sort slots within each date chronologically
    for (const list of map.values()) {
      list.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    }
    return map;
  }, [availabilities]);

  // Calendar grid matrix calculation for viewDate
  const calendarDays = useMemo(() => {
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();

    const firstDayIndex = new Date(year, month, 1).getDay();
    const totalDaysInMonth = new Date(year, month + 1, 0).getDate();
    const totalDaysPrevMonth = new Date(year, month, 0).getDate();

    const days = [];

    // 1. Previous month padding days
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const dayNum = totalDaysPrevMonth - i;
      const dateObj = new Date(year, month - 1, dayNum);
      const dateKey = toDateKey(dateObj);
      days.push({
        dateObj,
        dateKey,
        dayNum,
        isCurrentMonth: false,
        isPast: dateKey < todayKey,
        isToday: dateKey === todayKey,
      });
    }

    // 2. Current month days
    for (let dayNum = 1; dayNum <= totalDaysInMonth; dayNum++) {
      const dateObj = new Date(year, month, dayNum);
      const dateKey = toDateKey(dateObj);
      days.push({
        dateObj,
        dateKey,
        dayNum,
        isCurrentMonth: true,
        isPast: dateKey < todayKey,
        isToday: dateKey === todayKey,
      });
    }

    // 3. Next month padding days to complete 35 or 42 cells (full weeks)
    const totalCells = days.length <= 35 ? 35 : 42;
    const remaining = totalCells - days.length;
    for (let dayNum = 1; dayNum <= remaining; dayNum++) {
      const dateObj = new Date(year, month + 1, dayNum);
      const dateKey = toDateKey(dateObj);
      days.push({
        dateObj,
        dateKey,
        dayNum,
        isCurrentMonth: false,
        isPast: dateKey < todayKey,
        isToday: dateKey === todayKey,
      });
    }

    return days;
  }, [viewDate, todayKey]);

  // Month navigation handlers
  const handlePrevMonth = () => {
    setViewDate((prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setViewDate((prev) => new Date(prev.getFullYear(), prev.getMonth() + 1, 1));
  };

  const handleGoToday = () => {
    const now = new Date();
    setViewDate(new Date(now.getFullYear(), now.getMonth(), 1));
    setSelectedDate(todayKey);
    setFormError(null);
    setFormSuccess(null);
    setBatchErrors([]);
  };

  const handleSelectDate = (dateKey) => {
    setSelectedDate(dateKey);
    setFormError(null);
    setFormSuccess(null);
    setBatchErrors([]);
  };

  // Slot builder handlers: add, remove, edit slots for the selected date
  const handleAddSlot = () => {
    setFormError(null);
    setSlotsToSchedule((prev) => {
      let nextStart = '14:00';
      let nextEnd = '18:00';
      if (prev.length > 0) {
        const lastSlot = prev[prev.length - 1];
        if (lastSlot.endTime) {
          const [h, m] = lastSlot.endTime.split(':').map(Number);
          const startH = (h + 1) % 24;
          const endH = (startH + 3) % 24;
          nextStart = `${String(startH).padStart(2, '0')}:${String(m || 0).padStart(2, '0')}`;
          nextEnd = `${String(endH).padStart(2, '0')}:${String(m || 0).padStart(2, '0')}`;
        }
      }
      return [
        ...prev,
        { id: Date.now() + Math.random(), startTime: nextStart, endTime: nextEnd },
      ];
    });
  };

  const handleRemoveSlot = (idToRemove) => {
    setFormError(null);
    setSlotsToSchedule((prev) => {
      if (prev.length <= 1) {
        return [{ id: Date.now(), startTime: '09:00', endTime: '13:00' }];
      }
      return prev.filter((s) => s.id !== idToRemove);
    });
  };

  const handleSlotChange = (id, field, value) => {
    setFormError(null);
    setSlotsToSchedule((prev) =>
      prev.map((s) => (s.id === id ? { ...s, [field]: value } : s))
    );
  };

  // Quick preset helper
  const handleApplyPreset = (presetType) => {
    setFormError(null);
    setBatchErrors([]);
    if (presetType === 'morning') {
      setSlotsToSchedule([{ id: Date.now(), startTime: '09:00', endTime: '13:00' }]);
    } else if (presetType === 'afternoon') {
      setSlotsToSchedule([{ id: Date.now(), startTime: '14:00', endTime: '18:00' }]);
    } else if (presetType === 'split') {
      setSlotsToSchedule([
        { id: Date.now(), startTime: '09:00', endTime: '12:00' },
        { id: Date.now() + 1, startTime: '14:00', endTime: '18:00' },
      ]);
    } else if (presetType === 'fullday') {
      setSlotsToSchedule([{ id: Date.now(), startTime: '09:00', endTime: '17:00' }]);
    }
  };

  // Submit batch availability creation
  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (creating) return;

    setFormError(null);
    setFormSuccess(null);
    setBatchErrors([]);

    if (!selectedDate) {
      setFormError('Please select a calendar date first.');
      return;
    }

    if (slotsToSchedule.length === 0) {
      setFormError('Please configure at least one availability slot.');
      return;
    }

    // Client-side pre-validations
    const now = new Date();
    const payload = [];

    for (let i = 0; i < slotsToSchedule.length; i++) {
      const slot = slotsToSchedule[i];
      if (!slot.startTime || !slot.endTime) {
        setFormError(`Slot #${i + 1}: Both consultation start time and end time are required.`);
        return;
      }

      const isoStart = toIsoFromDateAndTime(selectedDate, slot.startTime);
      const isoEnd = toIsoFromDateAndTime(selectedDate, slot.endTime);
      const startDateObj = new Date(isoStart);
      const endDateObj = new Date(isoEnd);

      if (isNaN(startDateObj.getTime()) || isNaN(endDateObj.getTime())) {
        setFormError(`Slot #${i + 1}: Invalid date or time format.`);
        return;
      }

      if (startDateObj >= endDateObj) {
        setFormError(`Slot #${i + 1} (${slot.startTime} – ${slot.endTime}): End time must be strictly after start time.`);
        return;
      }

      if (startDateObj <= now) {
        setFormError(`Slot #${i + 1} (${slot.startTime}): Consultation interval must be scheduled in the future.`);
        return;
      }

      payload.push({
        startTime: isoStart,
        endTime: isoEnd,
        active: true,
      });
    }

    // Check for internal overlaps within the submission list
    for (let i = 0; i < payload.length; i++) {
      for (let j = i + 1; j < payload.length; j++) {
        const aStart = new Date(payload[i].startTime);
        const aEnd = new Date(payload[i].endTime);
        const bStart = new Date(payload[j].startTime);
        const bEnd = new Date(payload[j].endTime);
        if (aStart < bEnd && aEnd > bStart) {
          setFormError(`Slot #${i + 1} and Slot #${j + 1} have overlapping time intervals. Please adjust them.`);
          return;
        }
      }
    }

    setCreating(true);

    try {
      const response = await createBatchAvailability(token, { slots: payload });

      if (response.totalCreated > 0 && response.totalFailed === 0) {
        setFormSuccess(
          `Successfully saved all ${response.totalCreated} consultation slot(s) for ${formatDateOnly(selectedDate)}.`
        );
        setSlotsToSchedule([{ id: Date.now(), startTime: '09:00', endTime: '13:00' }]);
        setBatchErrors([]);
      } else if (response.totalCreated > 0 && response.totalFailed > 0) {
        setFormSuccess(
          `Successfully saved ${response.totalCreated} slot(s). ${response.totalFailed} slot(s) could not be saved.`
        );
        setBatchErrors(response.rejectedSlots || []);
        // Retain only rejected slot indices in the form so the doctor can adjust and re-submit
        const rejectedIndices = new Set((response.rejectedSlots || []).map((r) => r.index));
        setSlotsToSchedule((prev) => {
          const retained = prev.filter((_, idx) => rejectedIndices.has(idx));
          return retained.length > 0 ? retained : [{ id: Date.now(), startTime: '09:00', endTime: '13:00' }];
        });
      } else {
        setFormError(`None of the ${response.totalFailed} slot(s) could be saved. Please review the reasons below.`);
        setBatchErrors(response.rejectedSlots || []);
      }

      // Refresh doctor schedules across the portal if any slot succeeded
      if (response.totalCreated > 0 && onRefresh) {
        onRefresh();
      }
    } catch (err) {
      setFormError(err.message || 'Failed to save availability slots. Please verify the schedule intervals.');
    } finally {
      setCreating(false);
    }
  };

  // Slots for currently selected date
  const selectedDaySlots = slotsByDate.get(selectedDate) || [];
  const isSelectedDatePast = selectedDate < todayKey;
  const monthTitle = viewDate.toLocaleString(undefined, { month: 'long', year: 'numeric' });

  return (
    <div className="availability-calendar-card" id="doctor-availability-calendar">
      {/* Calendar View Header */}
      <div className="calendar-top-bar">
        <div>
          <h4 className="calendar-heading">Consultation Availability Planner</h4>
          <p className="calendar-subheading">
            Navigate dates, inspect scheduled consultation windows, and publish open hours for patient booking.
          </p>
        </div>
        <div className="calendar-nav-controls">
          <button
            type="button"
            className="cal-btn-today"
            onClick={handleGoToday}
            title="Jump to today's date"
          >
            Today
          </button>
          <div className="cal-month-stepper">
            <button
              type="button"
              className="cal-btn-arrow"
              onClick={handlePrevMonth}
              title="Previous month"
              aria-label="Previous month"
            >
              ‹
            </button>
            <span className="cal-current-month-label">{monthTitle}</span>
            <button
              type="button"
              className="cal-btn-arrow"
              onClick={handleNextMonth}
              title="Next month"
              aria-label="Next month"
            >
              ›
            </button>
          </div>
        </div>
      </div>

      {/* Main Grid: Month Calendar on Left, Selected Date Agenda on Right */}
      <div className="calendar-layout-grid">
        {/* Left Column: Month Matrix */}
        <div className="calendar-month-panel">
          <div className="calendar-weekdays-row">
            {DAYS_OF_WEEK.map((d) => (
              <span key={d} className="calendar-weekday-cell">
                {d}
              </span>
            ))}
          </div>

          <div className="calendar-days-grid" role="grid" aria-label="Availability Month Grid">
            {calendarDays.map((day) => {
              const daySlots = slotsByDate.get(day.dateKey) || [];
              const hasActiveSlots = daySlots.some((s) => s.active);
              const isSelected = day.dateKey === selectedDate;

              return (
                <button
                  type="button"
                  key={day.dateKey}
                  className={`calendar-day-cell ${
                    !day.isCurrentMonth ? 'day-other-month' : ''
                  } ${day.isPast ? 'day-past' : ''} ${
                    day.isToday ? 'day-today' : ''
                  } ${isSelected ? 'day-selected' : ''}`}
                  onClick={() => handleSelectDate(day.dateKey)}
                  aria-selected={isSelected}
                  title={`${formatDateOnly(day.dateKey)}: ${daySlots.length} slot(s)`}
                >
                  <span className="day-number-text">{day.dayNum}</span>

                  {/* Slot Indicators */}
                  {daySlots.length > 0 && (
                    <div className="day-slot-indicators">
                      <span
                        className={`slot-dot-pill ${
                          hasActiveSlots ? 'dot-active' : 'dot-inactive'
                        }`}
                      >
                        {daySlots.length} {daySlots.length === 1 ? 'slot' : 'slots'}
                      </span>
                    </div>
                  )}
                </button>
              );
            })}
          </div>

          <div className="calendar-legend-row">
            <div className="legend-item">
              <span className="legend-dot dot-active" />
              <span>Active Consultation Slot</span>
            </div>
            <div className="legend-item">
              <span className="legend-dot dot-inactive" />
              <span>Deactivated Slot</span>
            </div>
            <div className="legend-item">
              <span className="legend-ring" />
              <span>Selected Date</span>
            </div>
          </div>
        </div>

        {/* Right Column: Day Schedule & Quick Add Form */}
        <div className="calendar-agenda-panel">
          {/* Selected Date Header */}
          <div className="agenda-header">
            <div>
              <span className="agenda-eyebrow">Selected Schedule Date</span>
              <h5 className="agenda-date-title">{formatDateOnly(selectedDate)}</h5>
            </div>
            {selectedDate === todayKey && (
              <span className="badge-today-pill">Today</span>
            )}
          </div>

          {/* Form Feedback Messages */}
          {formSuccess && (
            <div className="cal-alert cal-alert-success" role="alert">
              <span>✓</span>
              <div>{formSuccess}</div>
              <button
                type="button"
                className="cal-alert-close"
                onClick={() => setFormSuccess(null)}
              >
                ✕
              </button>
            </div>
          )}

          {formError && (
            <div className="cal-alert cal-alert-error" role="alert">
              <span>⚠️</span>
              <div>{formError}</div>
              <button
                type="button"
                className="cal-alert-close"
                onClick={() => setFormError(null)}
              >
                ✕
              </button>
            </div>
          )}

          {/* Existing Slots on Selected Date */}
          <div className="agenda-slots-section">
            <div className="agenda-section-title-row">
              <span className="agenda-section-label">Published Availability on this Date</span>
              <span className="slots-count-badge">{selectedDaySlots.length}</span>
            </div>

            {selectedDaySlots.length === 0 ? (
              <div className="agenda-empty-state">
                <span className="empty-cal-icon">🕒</span>
                <p>No consultation slots published for this date.</p>
                <small>Use the form below to add available clinic hours.</small>
              </div>
            ) : (
              <div className="agenda-slots-list">
                {selectedDaySlots.map((slot) => {
                  const isDeactivating = actionLoadingKey === `deactivate-${slot.id}`;
                  const isActive = slot.active;

                  return (
                    <div
                      key={slot.id}
                      className={`agenda-slot-item ${
                        isActive ? 'slot-item-active' : 'slot-item-inactive'
                      }`}
                    >
                      <div className="slot-item-main">
                        <div className="slot-time-range">
                          <span className="time-clock-icon">⏱</span>
                          <span className="time-range-text">
                            {formatTimeOnly(slot.startTime)} – {formatTimeOnly(slot.endTime)}
                          </span>
                        </div>
                        <div className="slot-meta-tags">
                          <span className="slot-id-tag">Slot #{slot.id}</span>
                          <span
                            className={`slot-status-pill ${
                              isActive ? 'pill-active' : 'pill-inactive'
                            }`}
                          >
                            {isActive ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                      </div>

                      <div className="slot-item-action">
                        {isActive ? (
                          <button
                            type="button"
                            className="btn-deactivate-sm"
                            onClick={() => onDeactivateSlot(slot.id)}
                            disabled={isDeactivating}
                            title="Deactivate this availability slot"
                            id={`btn-deactivate-cal-slot-${slot.id}`}
                          >
                            {isDeactivating ? (
                              <>
                                <span className="spinner-cal-sm" />
                                Deactivating...
                              </>
                            ) : (
                              'Deactivate Slot'
                            )}
                          </button>
                        ) : (
                          <span className="slot-inactive-text">Deactivated</span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Add Availability Form on Selected Date */}
          <div className="agenda-add-section">
            <div className="agenda-section-title-row">
              <span className="agenda-section-label">
                + Pre-Schedule Availability for {formatDateOnly(selectedDate)}
              </span>
              <span className="slots-count-badge">
                {slotsToSchedule.length} {slotsToSchedule.length === 1 ? 'slot' : 'slots'} planned
              </span>
            </div>

            {isSelectedDatePast ? (
              <div className="past-date-notice">
                <span>ℹ️</span>
                <p>Selected date is in the past. Choose today or a future date to schedule availability.</p>
              </div>
            ) : (
              <form className="cal-add-form" onSubmit={handleCreateSubmit}>
                {/* Quick Presets */}
                <div className="preset-row">
                  <span className="preset-label">Quick Presets:</span>
                  <button
                    type="button"
                    className="btn-preset-chip"
                    onClick={() => handleApplyPreset('morning')}
                    disabled={creating}
                  >
                    Morning (09:00 - 13:00)
                  </button>
                  <button
                    type="button"
                    className="btn-preset-chip"
                    onClick={() => handleApplyPreset('afternoon')}
                    disabled={creating}
                  >
                    Afternoon (14:00 - 18:00)
                  </button>
                  <button
                    type="button"
                    className="btn-preset-chip"
                    onClick={() => handleApplyPreset('split')}
                    disabled={creating}
                  >
                    Split Shift (09-12 &amp; 14-18)
                  </button>
                  <button
                    type="button"
                    className="btn-preset-chip"
                    onClick={() => handleApplyPreset('fullday')}
                    disabled={creating}
                  >
                    Full Day (09:00 - 17:00)
                  </button>
                </div>

                {/* Batch Rejection Details if any */}
                {batchErrors.length > 0 && (
                  <div className="cal-batch-errors-card" role="alert">
                    <div className="batch-errors-header">
                      <span>⚠️</span>
                      <strong>Some slots could not be scheduled:</strong>
                    </div>
                    <ul className="batch-errors-list">
                      {batchErrors.map((err, idx) => (
                        <li key={idx} className="batch-error-item">
                          <span className="batch-error-slot-tag">Slot #{err.index + 1}</span>
                          {err.startTime && err.endTime && (
                            <span className="batch-error-time">
                              ({formatTimeOnly(err.startTime)} – {formatTimeOnly(err.endTime)}):
                            </span>
                          )}
                          <span className="batch-error-reason">{err.reason}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Multiple Time Slot Rows */}
                <div className="cal-slots-builder-list">
                  {slotsToSchedule.map((slot, index) => (
                    <div key={slot.id} className="cal-slot-builder-row">
                      <div className="cal-slot-row-header">
                        <span className="cal-slot-row-badge">Slot #{index + 1}</span>
                        {slotsToSchedule.length > 1 && (
                          <button
                            type="button"
                            className="btn-remove-slot-row"
                            onClick={() => handleRemoveSlot(slot.id)}
                            disabled={creating}
                            title="Remove this time slot"
                            aria-label={`Remove slot ${index + 1}`}
                          >
                            ✕ Remove
                          </button>
                        )}
                      </div>

                      <div className="cal-form-row">
                        <div className="cal-form-group">
                          <label className="cal-field-label" htmlFor={`cal-start-time-${slot.id}`}>
                            Start Time <span className="req">*</span>
                          </label>
                          <input
                            id={`cal-start-time-${slot.id}`}
                            type="time"
                            className="cal-time-input"
                            value={slot.startTime}
                            onChange={(e) => handleSlotChange(slot.id, 'startTime', e.target.value)}
                            disabled={creating}
                            required
                          />
                        </div>

                        <div className="cal-form-group">
                          <label className="cal-field-label" htmlFor={`cal-end-time-${slot.id}`}>
                            End Time <span className="req">*</span>
                          </label>
                          <input
                            id={`cal-end-time-${slot.id}`}
                            type="time"
                            className="cal-time-input"
                            value={slot.endTime}
                            onChange={(e) => handleSlotChange(slot.id, 'endTime', e.target.value)}
                            disabled={creating}
                            required
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Actions: Add Another Slot & Save All Slots */}
                <div className="cal-form-actions-row">
                  <button
                    type="button"
                    id="btn-add-time-slot"
                    className="btn-add-slot-row"
                    onClick={handleAddSlot}
                    disabled={creating}
                  >
                    + Add Another Slot
                  </button>

                  <button
                    type="submit"
                    id="btn-save-cal-availability"
                    className="btn-save-availability"
                    disabled={creating || slotsToSchedule.length === 0}
                  >
                    {creating ? (
                      <>
                        <span className="spinner-cal-sm" />
                        Saving Slots...
                      </>
                    ) : (
                      `Save Slots (${slotsToSchedule.length})`
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

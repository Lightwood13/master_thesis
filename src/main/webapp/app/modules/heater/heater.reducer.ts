import { Action, createAsyncThunk, createSlice, ThunkAction } from '@reduxjs/toolkit';
import axios from 'axios';
import { DateTime } from 'luxon';
import { IRootState } from 'app/config/store';
import { DateRangePickerValue } from '@tremor/react';

export type StatisticsType = 'temperature' | 'consumption' | 'price';

const initialState = {
  heater: null as Heater | null,
  statisticsType: 'temperature' as StatisticsType,
  statisticsDateRange: {
    from: DateTime.now().startOf('day').minus({ days: 7 }),
    to: DateTime.now().startOf('day'),
    selectValue: 'w',
  } as {
    from?: DateTime;
    to?: DateTime;
    selectValue?: string;
  },
  statistics: null as StatisticsData | null,
  models: [] as Model[],
};

export type HeaterState = Readonly<typeof initialState>;

export type Heater = {
  id: number;
  serial: string;
  roomTemperature: number | null;
  outsideTemperature: number | null;
  heaterPower: number | null;
  weekConsumption: number | null;
  schedule: string;
  calibrationStatus: string;
  calibrationStart: DateTime | null;
  calibrationEnd: DateTime | null;
  calibrationPercentage: number | null;
  activeModelId: number | null;
  savings: number | null;
};

export type Model = {
  id: number;
  name: string;
  targetTemperature: number;
  minTemperature: number;
  maxTemperature: number;
  createdOn: DateTime;
  status: string;
};

export type NewModelDTO = {
  serial: string;
  name: string;
  targetTemperature: number;
  minTemperature: number;
  maxTemperature: number;
  activateImmediately: boolean;
};

export type StatisticsData = StatisticsWindowData[];

export type StatisticsWindowData = {
  readonly periodStart: DateTime;
  readonly periodEnd: DateTime;
  readonly data: Record<StatisticsField, number | null>;
};

export type StatisticsRequest = {
  readonly serial: string;
  readonly fields: StatisticsField[];
  readonly startTime: DateTime;
  readonly endTime: DateTime;
  readonly aggregationPeriod: StatisticsAggregationPeriod;
  readonly timeZone: string;
};

export type StatisticsField = 'ROOM_TEMPERATURE' | 'OUTSIDE_TEMPERATURE' | 'ELECTRIC_CONSUMPTION' | 'ELECTRICITY_PRICE';

export type StatisticsAggregationPeriod = 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

function heaterUrl(serial: string): string {
  return `/api/heaters/${serial}`;
}

const statisticsUrl = '/api/statistics/calculate';

export const fetchHeater = createAsyncThunk('heater.fetch_heater', async (serial: string) => axios.get<Heater>(heaterUrl(serial)));

export const fetchStatistics = createAsyncThunk('heater.fetch_statistics', async (request: StatisticsRequest) =>
  axios.post<StatisticsData>(statisticsUrl, request)
);

export const fetchModels = createAsyncThunk('heater.fetch_models', async (serial: string) =>
  axios.get<Model[]>(heaterUrl(serial) + '/models')
);

export const startCalibration = createAsyncThunk('heater.start_calibration', async (serial: string) =>
  axios.post(heaterUrl(serial) + '/start-calibration')
);

export const createNewModel = createAsyncThunk('heater.create_new_model', async (data: NewModelDTO) =>
  axios.post(heaterUrl(data.serial) + '/models', data)
);

export const updateHeater =
  (serial: string): ThunkAction<void, IRootState, unknown, Action<any>> =>
  (dispatch, getState) => {
    if (serial === getState().heater.heater?.serial) {
      dispatch(fetchHeater(serial));
      dispatch(updateStatistics(serial));
      dispatch(fetchModels(serial));
    }
  };

export const updateStatistics =
  (serial: string): ThunkAction<void, IRootState, unknown, Action<any>> =>
  (dispatch, getState) => {
    const state = getState();
    const startTime = state.heater.statisticsDateRange.from ?? DateTime.fromISO('2024-05-14T00:00:00+03:00');
    const endTime = state.heater.statisticsDateRange.to ?? DateTime.fromISO('2024-05-14T00:00:00+03:00');
    let aggregationPeriod: StatisticsAggregationPeriod;
    switch (state.heater.statisticsDateRange.selectValue) {
      case 'w':
        aggregationPeriod = 'HOUR';
        break;
      default:
        aggregationPeriod = 'HOUR';
        break;
    }

    let fields: StatisticsField[];
    switch (state.heater.statisticsType) {
      case 'temperature':
        fields = ['ROOM_TEMPERATURE'];
        break;
      case 'consumption':
        fields = ['ELECTRIC_CONSUMPTION'];
        break;
      case 'price':
        fields = ['ELECTRICITY_PRICE'];
        break;
      default:
        fields = [];
        break;
    }

    dispatch(
      fetchStatistics({
        serial,
        fields,
        startTime,
        endTime: endTime.plus({ days: 1 }),
        aggregationPeriod,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
    );
  };

export const HeaterSlice = createSlice({
  name: 'heater',
  initialState: initialState as HeaterState,
  reducers: {
    setStatisticsType(state, action) {
      state.statisticsType = action.payload;
    },
    setStatisticsDateRange(state, action) {
      const newRange = action.payload as DateRangePickerValue;
      state.statisticsDateRange = {
        from: newRange.from ? DateTime.fromJSDate(newRange.from) : undefined,
        to: newRange.to ? DateTime.fromJSDate(newRange.to) : undefined,
        selectValue: newRange.selectValue,
      };
    },
  },
  extraReducers(builder) {
    builder.addCase(fetchHeater.fulfilled, (state, action) => {
      state.heater = action.payload.data;
    });
    builder.addCase(fetchStatistics.fulfilled, (state, action) => {
      state.statistics = action.payload.data;
    });
    builder.addCase(fetchModels.fulfilled, (state, action) => {
      state.models = action.payload.data;
    });
  },
});

export const { setStatisticsType, setStatisticsDateRange } = HeaterSlice.actions;

export default HeaterSlice.reducer;

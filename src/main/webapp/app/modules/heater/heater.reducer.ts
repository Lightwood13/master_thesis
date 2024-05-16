import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { DateTime } from 'luxon';

const initialState = {
  heater: null as Heater | null,
  statistics: null as StatisticsData | null,
};

export type HeaterState = Readonly<typeof initialState>;

export type Heater = {
  id: number;
  serial: string;
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

export enum StatisticsField {
  ELECTRIC_CONSUMPTION = 'ELECTRIC_CONSUMPTION',
  ROOM_TEMPERATURE = 'ROOM_TEMPERATURE',
}

export enum StatisticsAggregationPeriod {
  HOUR = 'HOUR',
  DAY = 'DAY',
  WEEK = 'WEEK',
  MONTH = 'MONTH',
}

function heaterUrl(serial: string): string {
  return `/api/heaters/${serial}`;
}

const statisticsUrl = '/api/statistics/calculate';

export const fetchHeater = createAsyncThunk('heater.fetch_heater', async (serial: string) => axios.get<Heater>(heaterUrl(serial)));

export const fetchStatistics = createAsyncThunk('heater.fetch_statistics', async (request: StatisticsRequest) =>
  axios.post<StatisticsData>(statisticsUrl, request)
);

export const HeaterSlice = createSlice({
  name: 'heater',
  initialState: initialState as HeaterState,
  reducers: {},
  extraReducers(builder) {
    builder.addCase(fetchHeater.fulfilled, (state, action) => {
      state.heater = action.payload.data;
    });
    builder.addCase(fetchStatistics.fulfilled, (state, action) => {
      state.statistics = action.payload.data;
    });
  },
});

export default HeaterSlice.reducer;

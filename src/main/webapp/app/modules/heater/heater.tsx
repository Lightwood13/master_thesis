import React, { ComponentType, ReactElement, SyntheticEvent, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ChartData, ChartOptions } from 'chart.js';
import 'chartjs-adapter-luxon';
import { Line } from 'react-chartjs-2';
import { DateTime } from 'luxon';
import { Card, Color, DateRangePicker, DateRangePickerValue, Icon, ProgressBar } from '@tremor/react';
import { Button, Grid, Tab, Tabs } from '@mui/material';
import { RiBrainLine, RiCheckboxCircleLine, RiHome8Line, RiProgress6Line, RiTempColdLine, RiTempHotLine } from '@remixicon/react';
import { PiEmpty } from 'react-icons/pi';
import { SlEnergy } from 'react-icons/sl';
import { LuBarChartBig } from 'react-icons/lu';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import {
  fetchHeater,
  fetchModels,
  fetchStatistics,
  Heater,
  Model,
  setStatisticsDateRange,
  setStatisticsType,
  startCalibration,
  StatisticsAggregationPeriod,
  StatisticsData,
  StatisticsField,
  StatisticsType,
  updateStatistics,
} from 'app/modules/heater/heater.reducer';
import ModelTable from 'app/modules/heater/table';

function str(value: number | null, unit: string): string {
  if (value === null) {
    return '-';
  } else {
    return `${value.toFixed(1)} ${unit}`;
  }
}

function StatisticsCard({ title, value, icon, color }: { title: string; value: string; icon: ComponentType; color: Color }) {
  return (
    <Card className="mx-auto max-w-xs h-full" decoration="top" decorationColor={color}>
      <div className="flex items-center space-x-6 h-full">
        <Icon icon={icon} color={color} variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">{title}</p>
          <p className="text-tremor-metric font-semibold text-tremor-content-strong">{value}</p>
        </div>
      </div>
    </Card>
  );
}

export const HeaterPage = () => {
  const heater = useAppSelector(state => state.heater.heater);
  const statistics: StatisticsData | null = useAppSelector(state => state.heater.statistics);
  const dateRange = useAppSelector(state => state.heater.statisticsDateRange);
  const statisticsType = useAppSelector(state => state.heater.statisticsType);

  const dispatch = useAppDispatch();

  const { serial } = useParams<'serial'>() as { serial: string };
  const [activeTab, setActiveTab] = useState(0);

  const handleTabChange = (event: SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
    let newStatisticsType: StatisticsType = 'temperature';
    switch (newValue) {
      case 0:
        newStatisticsType = 'temperature';
        break;
      case 1:
        newStatisticsType = 'consumption';
        break;
      case 2:
        newStatisticsType = 'price';
        break;
      default:
        newStatisticsType = 'temperature';
        break;
    }
    dispatch(setStatisticsType(newStatisticsType));
    dispatch(updateStatistics(serial));
  };

  const setDateRange = (newDateRange: DateRangePickerValue) => {
    dispatch(setStatisticsDateRange(newDateRange));
    dispatch(updateStatistics(serial));
  };

  useEffect(() => {
    dispatch(fetchHeater(serial));
    dispatch(updateStatistics(serial));
    dispatch(fetchModels(serial));
  }, []);

  if (heater === null) {
    return <p>Loading...</p>;
  }

  let statisticsElem: JSX.Element;
  if (statistics !== null) {
    const chartOptions: ChartOptions<'line'> = {
      animation: false,
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'day',
            // unit: 'hour',
            displayFormats: {
              day: 'dd MMM',
              hour: 'HH:mm',
            },
          },
        },
      },
    };

    if ('ROOM_TEMPERATURE' in statistics[0].data) {
      chartOptions.scales = {
        ...chartOptions.scales,
        y: {
          min: 18,
          max: 24,
        },
      };
    } else {
      chartOptions.scales = {
        ...chartOptions.scales,
        y: {
          min: 0,
        },
      };
    }

    const chartData: ChartData<'line'> = {
      labels: statistics.map(dataPoint => dataPoint.periodStart),
      datasets: [
        {
          label: 'Room Temperature',
          data: statistics.map(dataPoint => dataPoint.data['ROOM_TEMPERATURE']),
        },
        {
          label: 'Power consumption',
          data: statistics.map(dataPoint => dataPoint.data['ELECTRIC_CONSUMPTION']),
        },
        {
          label: 'Electricity price',
          data: statistics.map(dataPoint => dataPoint.data['ELECTRICITY_PRICE']),
        },
      ],
    };

    chartData.datasets = chartData.datasets.filter(dataset => !dataset.data.every(value => value === null || value === undefined));

    statisticsElem = (
      <div>
        <Line data={chartData} options={chartOptions} />
      </div>
    );
  } else {
    statisticsElem = <div></div>;
  }

  return (
    <div className="space-y-1">
      <div className="flex items-center">
        <Icon icon={RiHome8Line} size="xl" />
        <p className="text-3xl">Heater {heater.serial}</p>
      </div>

      <Grid container spacing={2}>
        <Grid item xs={7}>
          <Card className="space-y-4">
            <Tabs value={activeTab} onChange={handleTabChange} centered>
              <Tab label="Temperature" />
              <Tab label="Energy consumption" />
              <Tab label="Electricity price" />
              <Tab label="Cost" />
            </Tabs>
            <DateRangePicker
              className="mx-auto max-w-md"
              value={{
                from: dateRange.from?.toJSDate(),
                to: dateRange.to?.toJSDate(),
                selectValue: dateRange.selectValue,
              }}
              onValueChange={setDateRange}
            />
            {statisticsElem}
          </Card>
        </Grid>
        <Grid item xs={5}>
          <Grid container spacing={2} className="h-full" style={{ marginBottom: 29 }}>
            <Grid item xs={6}>
              <StatisticsCard title="Room temperature" value={str(heater.roomTemperature, '°C')} icon={RiTempHotLine} color="amber" />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard
                title="Outside temperature"
                value={str(heater.outsideTemperature, '°C')}
                icon={RiTempColdLine}
                color="indigo"
              />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard title="Heater power" value={str(heater.heaterPower, 'W')} icon={SlEnergy} color="green" />
            </Grid>
            <Grid item xs={6}>
              <StatisticsCard title="Consumption this week" value={str(heater.weekConsumption, 'kWh')} icon={LuBarChartBig} color="red" />
            </Grid>
            <ModelCard />
          </Grid>
        </Grid>
      </Grid>

      <ModelTable />
    </div>
  );
};

const ModelCard = () => {
  const heater = useAppSelector(state => state.heater.heater);
  const models: Model[] = useAppSelector(state => state.heater.models);
  const dispatch = useAppDispatch();

  const activeModel = models.find(model => model.id === heater?.activeModelId);

  let titleComponent: ReactElement = <div />;
  if (heater?.calibrationStatus === 'NOT_CALIBRATED') {
    titleComponent = (
      <div className="flex items-center space-x-6">
        <Icon icon={PiEmpty} color="red" variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">Status</p>
          <p className="text-tremor-title font-semibold text-tremor-content-strong">Not calibrated</p>
        </div>
      </div>
    );
  } else if (heater?.calibrationStatus === 'CALIBRATION_IN_PROGRESS') {
    titleComponent = (
      <div className="flex items-center space-x-6">
        <Icon icon={RiProgress6Line} color="yellow" variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">Status</p>
          <p className="text-tremor-title font-semibold text-tremor-content-strong">Gathering training data</p>
        </div>
      </div>
    );
  } else if (activeModel) {
    let modelStatusIcon: ReactElement;
    if (activeModel.status === 'Training') {
      modelStatusIcon = <Icon icon={RiProgress6Line} color="yellow" variant="shadow" size="lg" />;
    } else {
      modelStatusIcon = <Icon icon={RiCheckboxCircleLine} color="green" variant="shadow" size="lg" />;
    }

    titleComponent = (
      <div className="flex items-center space-x-6">
        <Icon icon={RiBrainLine} color="sky" variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">Active model</p>
          <p className="text-tremor-title font-semibold text-tremor-content-strong min-w-32">{activeModel.name}</p>
        </div>

        <div className="flex items-center space-x-6 pl-8">
          {modelStatusIcon}
          <div>
            <p className="text-tremor-default text-tremor-content">Status</p>
            <p className="text-tremor-title font-semibold text-tremor-content-strong">{activeModel.status}</p>
          </div>
        </div>
      </div>
    );
  } else {
    titleComponent = (
      <div className="flex items-center space-x-6">
        <Icon icon={PiEmpty} color="red" variant="shadow" size="lg" />
        <div>
          <p className="text-tremor-default text-tremor-content">Status</p>
          <p className="text-tremor-title font-semibold text-tremor-content-strong">No active model</p>
        </div>
      </div>
    );
  }

  let savings: number | null = null;
  if (activeModel?.status === 'Working') {
    savings = heater?.savings ?? null;
  }

  let modelStats: ReactElement = <div />;
  if (heater?.calibrationStatus === 'NOT_CALIBRATED') {
    modelStats = (
      <Button
        variant="contained"
        onClick={() => {
          dispatch(startCalibration(heater.serial));
        }}
      >
        Start calibration
      </Button>
    );
  } else if (heater?.calibrationStatus === 'CALIBRATION_IN_PROGRESS') {
    const calibrationProgress = heater?.calibrationPercentage ? Math.round(heater?.calibrationPercentage * 100) : 0;
    modelStats = (
      <div className="space-y-4">
        <p className="text-xl">Calibration progress: {calibrationProgress}%</p>
        <ProgressBar value={calibrationProgress} />
      </div>
    );
  } else if (activeModel) {
    modelStats = (
      <Grid container spacing={2}>
        <Grid item xs={6}>
          <div>
            <p className="text-tremor-default text-tremor-content">Target temperature</p>
            <p className="text-2xl font-semibold text-tremor-content-strong">{activeModel.targetTemperature} °C</p>
          </div>
        </Grid>
        <Grid item xs={6}>
          <div>
            <p className="text-tremor-default text-tremor-content">Allowed temperature range</p>
            <p className="text-2xl font-semibold text-tremor-content-strong">
              {activeModel.minTemperature}-{activeModel.maxTemperature} °C
            </p>
          </div>
        </Grid>
        <Grid item xs={6}>
          <div>
            <p className="text-tremor-default text-tremor-content">Active since</p>
            <p className="text-2xl font-semibold text-tremor-content-strong">{activeModel.createdOn.toFormat('dd MMM yyyy')}</p>
          </div>
        </Grid>
        <Grid item xs={6}>
          <div>
            <p className="text-tremor-default text-tremor-content">Estimated money savings</p>
            <p className="text-2xl font-semibold text-tremor-content-strong">{str(savings, '€')}</p>
          </div>
        </Grid>
      </Grid>
    );
  }

  return (
    <Grid item xs={12}>
      <Card className="space-y-4 h-full" decoration="top" decorationColor="sky">
        {titleComponent}
        {modelStats}
      </Card>
    </Grid>
  );
};

export default HeaterPage;

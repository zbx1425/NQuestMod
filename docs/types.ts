export interface Vec3d {
  x: number
  y: number
  z: number
}

export interface ManualTriggerCriterion {
  type: 'ManualTriggerCriterion'
  id: string
  description: string
}

export interface InBoundsCriterion {
  type: 'InBoundsCriterion'
  min: Vec3d
  max: Vec3d
  description: string
}

export interface RideLineCriterion {
  type: 'RideLineCriterion'
  lineName: string
}

export interface VisitStationCriterion {
  type: 'VisitStationCriterion'
  stationName: string
}

export interface RideToStationCriterion {
  type: 'RideToStationCriterion'
  stationName: string
}

export interface RideLineToStationCriterion {
  type: 'RideLineToStationCriterion'
  lineName: string
  stationName: string
}

export interface ConstantCriterion {
  type: 'ConstantCriterion'
  value: boolean
  description: string
}

export interface AndCriterion {
  type: 'AndCriterion'
  criteria: Criterion[]
}

export interface OrCriterion {
  type: 'OrCriterion'
  criteria: Criterion[]
}

export interface LatchingCriterion {
  type: 'LatchingCriterion'
  baseCriterion: Criterion
}

export interface RisingEdgeAndConditionCriterion {
  type: 'RisingEdgeAndConditionCriterion'
  triggerCriterion: Criterion
  conditionCriterion: Criterion
  descriptionSupplier: Criterion
}

export type Criterion =
  | ManualTriggerCriterion
  | InBoundsCriterion
  | RideLineCriterion
  | VisitStationCriterion
  | RideToStationCriterion
  | RideLineToStationCriterion
  | ConstantCriterion
  | AndCriterion
  | OrCriterion
  | LatchingCriterion
  | RisingEdgeAndConditionCriterion

export interface Step {
  criteria: Criterion[]
}

export interface Quest {
  id: string
  name: string
  description: string
  steps: Step[]
  questPoints: number
}

// src/types/tripPlan.ts
export interface RouteItem {
  title: string
  lat: number
  lng: number
  durationFromPrev: string
}

export interface TravelPlan {
  day: number
  route: RouteItem[]
}

# This script to be run in the documentation folder after the files (see below) are generated

library(stringr)
library(tidyverse)
library(dplyr)
library(ggplot2)

files <- list.files(pattern="remote_nodes_and_searcher_\\d+_\\d+")
per_edges <- c()
remote_nodes_per_edges <- c()
remote_both_per_edges <- c()
distance_counts <- c()
remote_nodes_distance_counts <- c()
remote_both_distance_counts <- c()
distance_total <- c()
remote_nodes_distance_total <- c()
remote_both_distance_total <- c()

process_file <- function(file_data, per_edges, distance_counts, distance_total) {
    for(i in 1:length(file_data$Distance)) {
        if (i == -1) i <- 1
        else i <- i + 1

        t <- distance_counts[file_data$Distance[i]]
        distance_counts[file_data$Distance[i]] <- 1
        if (!is.na(t) && length(t) > 0)
            distance_counts[file_data$Distance[i]] <- t + distance_counts[file_data$Distance[i]]

        t <- distance_total[file_data$Distance[i]]
        distance_total[file_data$Distance[i]] <- file_data$Time[i]
        if (!is.na(t) && length(t) > 0)
            distance_total[file_data$Distance[i]] <- t + distance_total[file_data$Distance[i]]
    }

    per_edges$Time[length(per_edges$Time) + 1] <- sum(file_data$Time) / length(file_data$Time)
    per_edges$Density[length(per_edges$Density) + 1] <- 2 * numbers[2] / (numbers[1] * (numbers[1] - 1))
    return (list("per_edges" = per_edges, "distance_counts" = distance_counts, "distance_total" = distance_total))
}

for (file in files) {
    numbers <- as.numeric(str_extract_all(file, '\\d+')[[1]])
    file_data <- read_table(file, n_max=50) %>% mutate (Time = as.numeric(Time)) %>% drop_na
    rtrn <- process_file(file_data, per_edges, distance_counts, distance_total)
    per_edges <- rtrn$per_edges
    distance_counts <- rtrn$distance_counts
    distance_total <- rtrn$distance_total
    file_data <- read_table(file, skip=51, n_max=50) %>% mutate (Time = as.numeric(Time)) %>% drop_na
    rtrn <- process_file(file_data, remote_nodes_per_edges, remote_nodes_distance_counts, remote_nodes_distance_total)
    remote_nodes_per_edges <- rtrn$per_edges
    remote_nodes_distance_counts <- rtrn$distance_counts
    remote_nodes_distance_total <- rtrn$distance_total
    file_data <- read_table(file, skip=102, n_max=50) %>% mutate (Time = as.numeric(Time)) %>% drop_na
    rtrn <- process_file(file_data, remote_both_per_edges, remote_both_distance_counts, remote_both_distance_total)
    remote_both_per_edges <- rtrn$per_edges
    remote_both_distance_counts <- rtrn$distance_counts
    remote_both_distance_total <- rtrn$distance_total
}

per_edges <- per_edges %>% as_tibble %>% arrange(Density)
ggplot(NULL, aes(x=Density, y=Time)) +
    geom_point(data=as.data.frame(per_edges), aes(color="Time")) +
    geom_point(data=as.data.frame(remote_nodes_per_edges), aes(color="Remote nodes time")) +
    geom_point(data=as.data.frame(remote_both_per_edges), aes(color="Remote both time"))
ggsave("remote_nodes_and_searcher1.png")


distance_average <- ifelse(distance_counts > 0, distance_total / distance_counts, NA)
remote_nodes_distance_average <- ifelse(remote_nodes_distance_counts > 0, remote_nodes_distance_total / remote_nodes_distance_counts, NA)
remote_both_distance_average <- ifelse(remote_both_distance_counts > 0, remote_both_distance_total / remote_both_distance_counts, NA)

local_data = cbind(Distance = (1:(distance_average %>% length)) - 1, Average_time = distance_average)
remote_nodes_data = cbind(Distance = (1:(remote_nodes_distance_average %>% length)) - 1, Average_time = remote_nodes_distance_average)
remote_both_data = cbind(Distance = (1:(remote_both_distance_average %>% length)) - 1, Average_time = remote_both_distance_average)

ggplot(NULL, aes(x=Distance, y=Average_time)) +
    geom_point(data=as.data.frame(local_data), aes(color="Average local time")) +
    geom_point(data=as.data.frame(remote_nodes_data), aes(color="Average remote nodes time")) +
    geom_point(data=as.data.frame(remote_both_data), aes(color="Average remote both time")) +
    labs(x="Distance (0 denotes infinity)", y="Average time")
ggsave("remote_nodes_and_searcher2.png")

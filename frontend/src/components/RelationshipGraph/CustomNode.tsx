import React from 'react';
import {
    Paper,
    Box,
    Typography,
    Avatar,
    useTheme,
    alpha,
    Theme,
} from "@mui/material";
import CakeIcon from "@mui/icons-material/Cake";
import WcIcon from "@mui/icons-material/Wc";
import { Handle, Position, NodeProps } from "reactflow";
import { GRAPH_NODE_WIDTH, GRAPH_NODE_HEIGHT } from '../../config/graphConfig';

interface CustomNodeData {
    name: string;
    gender?: string;
    birthYear?: number;
    deathYear?: number;
    isSource?: boolean;
    isTarget?: boolean;
}

const CustomNode: React.FC<NodeProps<CustomNodeData>> = ({ data }) => {
    const theme = useTheme();

    const getGenderColor = (gender?: string): string => {
        if (!gender) return theme.palette.grey[400];
        if (gender.toLowerCase().includes("erkek")) {
            return theme.palette.mode === "dark" ? theme.palette.info.light : theme.palette.info.main;
        }
        if (gender.toLowerCase().includes("kadın")) {
            return theme.palette.mode === "dark" ? theme.palette.secondary.light : theme.palette.secondary.main;
        }
        return theme.palette.grey[400];
    };

    const isHighlighted = data.isSource || data.isTarget;

    return (
        <Paper
            elevation={isHighlighted ? 6 : 3}
            sx={{
                width: GRAPH_NODE_WIDTH,
                height: GRAPH_NODE_HEIGHT,
                display: "flex",
                flexDirection: "column",
                padding: theme.spacing(1.75),
                borderRadius: 2,
                textAlign: "center",
                background: isHighlighted
                    ? (theme.palette.mode === "dark" ? "#1B4332" : "#2D6A4F")
                    : (theme.palette.mode === "dark" ? alpha(theme.palette.background.paper, 0.7) : alpha(theme.palette.background.default, 0.7)),
                border: `1px solid ${isHighlighted
                    ? (theme.palette.mode === "dark" ? "#2D6A4F" : "#40916C")
                    : (theme.palette.mode === "dark" ? alpha(theme.palette.divider, 0.2) : alpha(theme.palette.divider, 0.4))
                }`,
                color: isHighlighted ? "#FFFFFF" : theme.palette.text.primary,
                fontFamily: theme.typography.fontFamily,
                overflow: "hidden",
                boxShadow: isHighlighted
                    ? `0 4px 10px ${alpha(theme.palette.common.black, 0.3)}`
                    : `0 2px 6px ${alpha(theme.palette.common.black, 0.1)}`,
                position: "relative",
                transition: "all 0.2s ease-in-out",
                "&:hover": {
                    transform: "translateY(-3px)",
                    boxShadow: isHighlighted
                        ? `0 6px 12px ${alpha(theme.palette.common.black, 0.4)}`
                        : `0 4px 8px ${alpha(theme.palette.common.black, 0.2)}`,
                },
            }}
        >
            <Handle
                type="target"
                position={Position.Left}
                style={{
                    background: theme.palette.primary.main,
                    width: 10,
                    height: 10,
                    border: `2px solid ${theme.palette.background.paper}`,
                    borderRadius: "50%",
                    boxShadow: `0 0 4px ${alpha(theme.palette.common.black, 0.3)}`,
                }}
                isConnectable={true}
            />

            <Box
                sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "flex-start",
                    mb: 1,
                    pb: 0.5,
                    borderBottom: `1px solid ${isHighlighted ? alpha("#FFFFFF", 0.25) : alpha(theme.palette.divider, 0.2)}`,
                    width: "100%",
                }}
            >
                <Avatar
                    sx={{
                        width: 36,
                        height: 36,
                        mr: 1.5,
                        bgcolor: getGenderColor(data.gender),
                        fontSize: "1rem",
                        fontWeight: "600",
                        color: "#fff",
                        boxShadow: `0 2px 8px ${alpha(getGenderColor(data.gender), 0.4)}`,
                    }}
                >
                    {data.name?.charAt(0).toUpperCase()}
                </Avatar>
                <Typography
                    variant="h6"
                    fontWeight="bold"
                    sx={{
                        fontSize: "1rem",
                        color: "#FFFFFF",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                        letterSpacing: "0.3px",
                        textAlign: "left",
                    }}
                >
                    {data.name}
                </Typography>
            </Box>

            <Box
                sx={{
                    width: "100%",
                    display: "flex",
                    flexDirection: "column",
                    gap: 0.8,
                    flexGrow: 1,
                    justifyContent: "center",
                    alignItems: "flex-start",
                    mt: 0.5,
                    px: 1,
                    color: isHighlighted ? alpha("#FFFFFF", 0.9) : theme.palette.text.secondary,
                }}
            >
                {data.gender && (
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, fontSize: "0.8rem", fontWeight: "500" }}>
                        <WcIcon sx={{ fontSize: "0.9rem" }} />
                        {data.gender}
                    </Box>
                )}
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, fontSize: "0.8rem", fontWeight: "500" }}>
                    <CakeIcon sx={{ fontSize: "0.9rem" }} />
                    <Typography component="span" sx={{ fontSize: "0.8rem" }}>
                        {data.birthYear ? `Doğum: ${data.birthYear}` : "Doğum: ?"}
                        {data.deathYear ? ` - Ölüm: ${data.deathYear}` : ""}
                    </Typography>
                </Box>
            </Box>

            <Handle
                type="source"
                position={Position.Right}
                style={{
                    background: theme.palette.primary.main,
                    width: 10,
                    height: 10,
                    border: `2px solid ${theme.palette.background.paper}`,
                    borderRadius: "50%",
                    boxShadow: `0 0 4px ${alpha(theme.palette.common.black, 0.3)}`,
                }}
                isConnectable={true}
            />
        </Paper>
    );
};

export default CustomNode;
